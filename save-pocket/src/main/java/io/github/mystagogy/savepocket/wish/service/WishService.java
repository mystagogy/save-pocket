package io.github.mystagogy.savepocket.wish.service;

import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import io.github.mystagogy.savepocket.notification.messaging.NotificationEventPublisher;
import io.github.mystagogy.savepocket.notification.messaging.PriceDropNotificationMessage;
import io.github.mystagogy.savepocket.report.service.ReportCacheService;
import io.github.mystagogy.savepocket.wish.dto.WishCreateRequest;
import io.github.mystagogy.savepocket.wish.dto.WishCreateResponse;
import io.github.mystagogy.savepocket.wish.dto.WishDetailResponse;
import io.github.mystagogy.savepocket.wish.dto.WishEventItem;
import io.github.mystagogy.savepocket.wish.dto.WishPriceHistoryItem;
import io.github.mystagogy.savepocket.wish.dto.WishSearchItemResponse;
import io.github.mystagogy.savepocket.wish.dto.WishStatusUpdateResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSummaryResponse;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingProduct;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingSearchClient;
import io.github.mystagogy.savepocket.wish.entity.PriceHistory;
import io.github.mystagogy.savepocket.wish.entity.PriceType;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishEventHistory;
import io.github.mystagogy.savepocket.wish.entity.WishEventType;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.repository.PriceHistoryRepository;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import io.github.mystagogy.savepocket.wish.repository.WishEventHistoryRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WishService {

    private static final long EXPIRE_HOURS = 72L;
    private static final List<String> TRACKED_PRODUCT_ID_QUERY_KEYS = List.of("id", "nvMid", "productNo");

    private final ProductWishRepository productWishRepository;
    private final WishEventHistoryRepository wishEventHistoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UserRepository userRepository;
    private final NaverShoppingSearchClient naverShoppingSearchClient;
    private final ReportCacheService reportCacheService;
    private final NotificationEventPublisher notificationEventPublisher;

    public WishService(
            ProductWishRepository productWishRepository,
            WishEventHistoryRepository wishEventHistoryRepository,
            PriceHistoryRepository priceHistoryRepository,
            UserRepository userRepository,
            NaverShoppingSearchClient naverShoppingSearchClient,
            ReportCacheService reportCacheService,
            NotificationEventPublisher notificationEventPublisher
    ) {
        this.productWishRepository = productWishRepository;
        this.wishEventHistoryRepository = wishEventHistoryRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.userRepository = userRepository;
        this.naverShoppingSearchClient = naverShoppingSearchClient;
        this.reportCacheService = reportCacheService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional
    public WishCreateResponse createWish(Long userId, WishCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SavePocketException(ErrorCode.UNAUTHORIZED));

        LocalDateTime now = LocalDateTime.now();
        ProductWish wish = new ProductWish();
        wish.setUser(user);
        wish.setProductName(request.productName());
        wish.setProductUrl(request.productUrl());
        wish.setTrackedProductId(resolveTrackedProductId(request.trackedProductId(), request.productUrl()));
        wish.setProductImageUrl(request.productImageUrl());
        wish.setMemo(request.memo());
        wish.setReferencePrice(request.referencePrice());
        wish.setUserDealPrice(request.userDealPrice());
        wish.setDealUrl(request.dealUrl());
        wish.setDealSourceType(request.dealSourceType());
        wish.setStatus(WishStatus.WAITING);
        wish.setFirstRegisteredAt(now);
        wish.setLastViewedAt(now);
        wish.setExpireAt(now.plusHours(EXPIRE_HOURS));
        wish.setReactivatedCount(0);

        ProductWish savedWish = productWishRepository.save(wish);

        WishEventHistory event = new WishEventHistory();
        event.setWish(savedWish);
        event.setEventType(WishEventType.REGISTERED);
        event.setEventAt(now);
        wishEventHistoryRepository.save(event);

        return new WishCreateResponse(
                savedWish.getId(),
                savedWish.getProductName(),
                savedWish.getProductUrl(),
                savedWish.getProductImageUrl(),
                savedWish.getReferencePrice(),
                savedWish.getUserDealPrice(),
                savedWish.effectivePrice(),
                savedWish.getStatus(),
                savedWish.getLastViewedAt(),
                savedWish.getExpireAt(),
                savedWish.getReactivatedCount()
        );
    }

    @Transactional(readOnly = true)
    public List<WishSearchItemResponse> searchWishes(String query) {
        if (!StringUtils.hasText(query)) {
            throw new SavePocketException(ErrorCode.VALIDATION_FAILED, "검색어는 필수입니다.");
        }

        return naverShoppingSearchClient.searchProducts(query).stream()
                .map(this::toSearchItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WishSummaryResponse> getWishes(Long userId, WishStatus status) {
        List<ProductWish> wishes;
        if (status != null) {
            wishes = productWishRepository.findByUser_IdAndStatusOrderByUpdatedAtDesc(userId, status);
        } else {
            wishes = productWishRepository.findByUser_IdAndStatusInOrderByUpdatedAtDesc(userId, Arrays.asList(WishStatus.values()));
        }

        return wishes.stream()
                .map(wish -> new WishSummaryResponse(
                        wish.getId(),
                        wish.getProductName(),
                        wish.getProductImageUrl(),
                        wish.getStatus(),
                        wish.effectivePrice(),
                        wish.getExpireAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public WishDetailResponse getWishDetail(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);

        List<WishPriceHistoryItem> priceHistories = priceHistoryRepository.findTop20ByWishIdOrderByChangedAtDesc(wishId).stream()
                .map(this::toPriceHistoryItem)
                .toList();

        List<WishEventItem> events = wishEventHistoryRepository.findTop50ByWishIdOrderByEventAtDesc(wishId).stream()
                .map(this::toEventItem)
                .toList();

        return new WishDetailResponse(
                wish.getId(),
                wish.getProductName(),
                wish.getProductUrl(),
                wish.getProductImageUrl(),
                wish.getMemo(),
                wish.getReferencePrice(),
                wish.getUserDealPrice(),
                wish.effectivePrice(),
                wish.getStatus(),
                wish.getLastViewedAt(),
                wish.getExpireAt(),
                wish.getReactivatedCount(),
                priceHistories,
                events
        );
    }

    @Transactional
    public WishStatusUpdateResponse purchaseWish(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);
        validateStatusTransition(wish.getStatus(), WishStatus.PURCHASED);
        LocalDateTime previousUpdatedAt = wish.getUpdatedAt();
        LocalDateTime previousExpiredAt = wish.getExpiredAt();
        LocalDateTime now = LocalDateTime.now();
        wish.setStatus(WishStatus.PURCHASED);
        appendEvent(wish, WishEventType.PURCHASED, now);
        productWishRepository.flush();
        evictMonthlySavingsCache(userId, resolveCurrentUpdatedAt(wish, now), previousUpdatedAt, previousExpiredAt);
        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public WishStatusUpdateResponse deleteWish(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);
        validateStatusTransition(wish.getStatus(), WishStatus.DELETED);
        LocalDateTime previousUpdatedAt = wish.getUpdatedAt();
        LocalDateTime previousExpiredAt = wish.getExpiredAt();
        LocalDateTime now = LocalDateTime.now();
        wish.setStatus(WishStatus.DELETED);
        appendEvent(wish, WishEventType.DELETED, now);
        productWishRepository.flush();
        evictMonthlySavingsCache(userId, resolveCurrentUpdatedAt(wish, now), previousUpdatedAt, previousExpiredAt);
        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public WishStatusUpdateResponse reactivateWish(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);
        validateReactivationStatus(wish.getStatus());

        LocalDateTime previousUpdatedAt = wish.getUpdatedAt();
        LocalDateTime previousExpiredAt = wish.getExpiredAt();
        LocalDateTime now = LocalDateTime.now();
        wish.setStatus(WishStatus.WAITING);
        wish.setLastViewedAt(now);
        wish.setExpireAt(now.plusHours(EXPIRE_HOURS));
        wish.setExpiredAt(null);
        wish.setSavedAmount(null);
        wish.setReactivatedCount(wish.getReactivatedCount() + 1);
        appendEvent(wish, WishEventType.REACTIVATED, now);
        productWishRepository.flush();
        evictMonthlySavingsCache(userId, resolveCurrentUpdatedAt(wish, now), previousUpdatedAt, previousExpiredAt);

        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public int expireDueWishes(LocalDateTime targetTime) {
        List<ProductWish> dueWishes = productWishRepository.findByStatusAndExpireAtLessThanEqual(
                WishStatus.WAITING,
                targetTime
        );

        Set<UserMonthCacheKey> evictTargets = new HashSet<>();

        for (ProductWish wish : dueWishes) {
            wish.setStatus(WishStatus.EXPIRED);
            wish.setExpiredAt(targetTime);
            wish.setSavedAmount(wish.effectivePrice());
            appendEvent(wish, WishEventType.EXPIRED, targetTime);
            evictTargets.add(new UserMonthCacheKey(wish.getUser().getId(), YearMonth.from(targetTime)));
        }

        for (UserMonthCacheKey evictTarget : evictTargets) {
            reportCacheService.evictMonthlySavingsAfterCommit(
                    evictTarget.userId(),
                    evictTarget.yearMonth().getYear(),
                    evictTarget.yearMonth().getMonthValue()
            );
        }

        return dueWishes.size();
    }

    @Transactional
    public PriceRefreshResult refreshLowestReferencePrices(LocalDateTime targetTime) {
        List<ProductWish> waitingWishes = productWishRepository.findByStatus(WishStatus.WAITING);

        int scannedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ProductWish wish : waitingWishes) {
            scannedCount++;
            try {
                Long latestLowestPrice = resolveLatestLowestPrice(wish);
                if (latestLowestPrice == null) {
                    skippedCount++;
                    continue;
                }

                Long previousReferencePrice = wish.getReferencePrice();
                Long historicalLowestBeforeUpdate = resolveHistoricalLowestReferencePrice(wish, previousReferencePrice);
                if (Objects.equals(previousReferencePrice, latestLowestPrice)) {
                    skippedCount++;
                    continue;
                }

                wish.setReferencePrice(latestLowestPrice);
                appendEvent(
                        wish,
                        WishEventType.PRICE_CHANGED,
                        targetTime,
                        "기준가 자동 갱신: " + previousReferencePrice + " -> " + latestLowestPrice
                );

                if (previousReferencePrice != null) {
                    appendPriceHistory(
                            wish,
                            PriceType.REFERENCE,
                            previousReferencePrice,
                            latestLowestPrice,
                            targetTime
                    );
                }

                if (isLowestPriceDrop(previousReferencePrice, latestLowestPrice, historicalLowestBeforeUpdate)
                        && canPublishNotification(wish)) {
                    notificationEventPublisher.publishPriceDropAfterCommit(new PriceDropNotificationMessage(
                            wish.getUser().getId(),
                            wish.getId(),
                            previousReferencePrice,
                            latestLowestPrice,
                            targetTime
                    ));
                }

                updatedCount++;
            } catch (RuntimeException ex) {
                failedCount++;
            }
        }

        return new PriceRefreshResult(scannedCount, updatedCount, skippedCount, failedCount);
    }

    private ProductWish getAccessibleWish(Long userId, Long wishId) {
        return productWishRepository.findByIdAndUser_Id(wishId, userId)
                .orElseThrow(() -> {
                    if (productWishRepository.existsById(wishId)) {
                        return new SavePocketException(ErrorCode.FORBIDDEN_RESOURCE);
                    }
                    return new SavePocketException(ErrorCode.WISH_NOT_FOUND);
                });
    }

    private void validateStatusTransition(WishStatus currentStatus, WishStatus nextStatus) {
        boolean allowed = (currentStatus == WishStatus.WAITING || currentStatus == WishStatus.EXPIRED)
                && (nextStatus == WishStatus.PURCHASED || nextStatus == WishStatus.DELETED);
        if (!allowed) {
            throw new SavePocketException(
                    ErrorCode.INVALID_WISH_STATE,
                    "현재 상태(" + currentStatus + ")에서는 " + nextStatus + " 처리할 수 없습니다."
            );
        }
    }

    private void validateReactivationStatus(WishStatus currentStatus) {
        if (currentStatus == WishStatus.EXPIRED) {
            return;
        }

        throw new SavePocketException(
                ErrorCode.INVALID_WISH_STATE,
                "현재 상태(" + currentStatus + ")에서는 WAITING 처리할 수 없습니다."
        );
    }

    private void appendEvent(ProductWish wish, WishEventType eventType, LocalDateTime eventAt) {
        appendEvent(wish, eventType, eventAt, null);
    }

    private void appendEvent(ProductWish wish, WishEventType eventType, LocalDateTime eventAt, String description) {
        WishEventHistory event = new WishEventHistory();
        event.setWish(wish);
        event.setEventType(eventType);
        event.setEventAt(eventAt);
        event.setDescription(description);
        wishEventHistoryRepository.save(event);
    }

    private void appendPriceHistory(
            ProductWish wish,
            PriceType priceType,
            Long previousPrice,
            Long changedPrice,
            LocalDateTime changedAt
    ) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setWish(wish);
        priceHistory.setPriceType(priceType);
        priceHistory.setPreviousPrice(previousPrice);
        priceHistory.setChangedPrice(changedPrice);
        priceHistory.setChangedAt(changedAt);
        priceHistoryRepository.save(priceHistory);
    }

    private void evictMonthlySavingsCache(Long userId, LocalDateTime... candidateDateTimes) {
        Set<YearMonth> yearMonths = new HashSet<>();
        for (LocalDateTime candidateDateTime : candidateDateTimes) {
            if (candidateDateTime == null) {
                continue;
            }
            yearMonths.add(YearMonth.from(candidateDateTime));
        }

        for (YearMonth yearMonth : yearMonths) {
            reportCacheService.evictMonthlySavingsAfterCommit(
                    userId,
                    yearMonth.getYear(),
                    yearMonth.getMonthValue()
            );
        }
    }

    private LocalDateTime resolveCurrentUpdatedAt(ProductWish wish, LocalDateTime fallback) {
        return wish.getUpdatedAt() != null ? wish.getUpdatedAt() : fallback;
    }

    private Long resolveLatestLowestPrice(ProductWish wish) {
        String trackedProductId = wish.getTrackedProductId();
        if (!StringUtils.hasText(trackedProductId)) {
            trackedProductId = extractProductIdFromUrl(wish.getProductUrl());
            if (StringUtils.hasText(trackedProductId)) {
                wish.setTrackedProductId(trackedProductId);
            }
        }

        if (!StringUtils.hasText(trackedProductId)) {
            return null;
        }

        final String finalTrackedProductId = trackedProductId;

        return naverShoppingSearchClient.searchProducts(finalTrackedProductId).stream()
                .filter(product -> isTrackedProductMatched(product, finalTrackedProductId))
                .map(NaverShoppingProduct::lowestPrice)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
    }

    private String resolveTrackedProductId(String requestTrackedProductId, String productUrl) {
        String urlDerivedProductId = extractProductIdFromUrl(productUrl);
        if (StringUtils.hasText(urlDerivedProductId)) {
            return urlDerivedProductId;
        }

        if (StringUtils.hasText(requestTrackedProductId)) {
            return requestTrackedProductId.trim();
        }
        return null;
    }

    private boolean isTrackedProductMatched(NaverShoppingProduct product, String trackedProductId) {
        if (trackedProductId.equals(product.productId())) {
            return true;
        }
        String productIdFromUrl = extractProductIdFromUrl(product.link());
        return trackedProductId.equals(productIdFromUrl);
    }

    private String extractProductIdFromUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return null;
        }

        String normalizedUrl = rawUrl.replace("&amp;", "&").trim();
        if (normalizedUrl.startsWith("//")) {
            normalizedUrl = "https:" + normalizedUrl;
        }

        try {
            URI uri = URI.create(normalizedUrl);
            String fromQuery = extractProductIdFromQuery(uri.getRawQuery());
            if (StringUtils.hasText(fromQuery)) {
                return fromQuery;
            }
            String fromPath = extractProductIdFromPath(uri.getPath());
            if (StringUtils.hasText(fromPath)) {
                return fromPath;
            }
        } catch (RuntimeException ignored) {
            // URL 형식이 예상과 달라도 전체 문자열에서 마지막 숫자 시퀀스를 찾기 위해 아래 fallback을 사용한다.
        }

        return extractProductIdFromPath(normalizedUrl);
    }

    private String extractProductIdFromQuery(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }

        for (String pair : rawQuery.split("&")) {
            int delimiterIndex = pair.indexOf('=');
            if (delimiterIndex <= 0 || delimiterIndex >= pair.length() - 1) {
                continue;
            }
            String key = decodeUrlComponent(pair.substring(0, delimiterIndex));
            String value = decodeUrlComponent(pair.substring(delimiterIndex + 1));
            if (!containsIgnoreCase(TRACKED_PRODUCT_ID_QUERY_KEYS, key)) {
                continue;
            }
            if (!StringUtils.hasText(value)) {
                continue;
            }
            return value.trim();
        }
        return null;
    }

    private String extractProductIdFromPath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }

        String[] tokens = path.split("/");
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if (!"catalog".equalsIgnoreCase(token)
                    && !"products".equalsIgnoreCase(token)
                    && !"item".equalsIgnoreCase(token)) {
                continue;
            }
            String candidate = tokens[i + 1].trim();
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private String decodeUrlComponent(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private boolean containsIgnoreCase(List<String> candidates, String target) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private Long resolveHistoricalLowestReferencePrice(ProductWish wish, Long previousReferencePrice) {
        Long historyLowest = null;
        if (wish.getId() != null) {
            historyLowest = priceHistoryRepository.findMinChangedPriceByWishIdAndPriceType(
                    wish.getId(),
                    PriceType.REFERENCE
            );
        }
        if (previousReferencePrice == null) {
            return historyLowest;
        }
        if (historyLowest == null) {
            return previousReferencePrice;
        }
        return Math.min(previousReferencePrice, historyLowest);
    }

    private boolean isLowestPriceDrop(Long previousReferencePrice, Long latestLowestPrice, Long historicalLowestBeforeUpdate) {
        if (previousReferencePrice == null || latestLowestPrice == null || historicalLowestBeforeUpdate == null) {
            return false;
        }
        return latestLowestPrice < previousReferencePrice && latestLowestPrice < historicalLowestBeforeUpdate;
    }

    private boolean canPublishNotification(ProductWish wish) {
        return wish.getId() != null && wish.getUser() != null && wish.getUser().getId() != null;
    }

    private record UserMonthCacheKey(Long userId, YearMonth yearMonth) {
    }

    public record PriceRefreshResult(
            int scannedCount,
            int updatedCount,
            int skippedCount,
            int failedCount
    ) {
    }

    private WishPriceHistoryItem toPriceHistoryItem(PriceHistory priceHistory) {
        return new WishPriceHistoryItem(
                priceHistory.getId(),
                priceHistory.getPriceType(),
                priceHistory.getPreviousPrice(),
                priceHistory.getChangedPrice(),
                priceHistory.getChangedAt()
        );
    }

    private WishEventItem toEventItem(WishEventHistory event) {
        return new WishEventItem(
                event.getId(),
                event.getEventType(),
                event.getEventAt(),
                event.getDescription(),
                event.getMetadata()
        );
    }

    private WishSearchItemResponse toSearchItem(NaverShoppingProduct product) {
        return new WishSearchItemResponse(
                product.title(),
                product.link(),
                product.productId(),
                product.image(),
                product.lowestPrice(),
                product.mallName()
        );
    }
}
