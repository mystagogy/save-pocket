package io.github.mystagogy.savepocket.wish.service;

import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
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
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishEventHistory;
import io.github.mystagogy.savepocket.wish.entity.WishEventType;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.repository.PriceHistoryRepository;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import io.github.mystagogy.savepocket.wish.repository.WishEventHistoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishService {

    private static final long EXPIRE_HOURS = 72L;

    private final ProductWishRepository productWishRepository;
    private final WishEventHistoryRepository wishEventHistoryRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UserRepository userRepository;
    private final NaverShoppingSearchClient naverShoppingSearchClient;

    public WishService(
            ProductWishRepository productWishRepository,
            WishEventHistoryRepository wishEventHistoryRepository,
            PriceHistoryRepository priceHistoryRepository,
            UserRepository userRepository,
            NaverShoppingSearchClient naverShoppingSearchClient
    ) {
        this.productWishRepository = productWishRepository;
        this.wishEventHistoryRepository = wishEventHistoryRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.userRepository = userRepository;
        this.naverShoppingSearchClient = naverShoppingSearchClient;
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
        wish.setStatus(WishStatus.PURCHASED);
        appendEvent(wish, WishEventType.PURCHASED, LocalDateTime.now());
        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public WishStatusUpdateResponse deleteWish(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);
        validateStatusTransition(wish.getStatus(), WishStatus.DELETED);
        wish.setStatus(WishStatus.DELETED);
        appendEvent(wish, WishEventType.DELETED, LocalDateTime.now());
        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public WishStatusUpdateResponse reactivateWish(Long userId, Long wishId) {
        ProductWish wish = getAccessibleWish(userId, wishId);
        validateReactivationStatus(wish.getStatus());

        LocalDateTime now = LocalDateTime.now();
        wish.setStatus(WishStatus.WAITING);
        wish.setLastViewedAt(now);
        wish.setExpireAt(now.plusHours(EXPIRE_HOURS));
        wish.setExpiredAt(null);
        wish.setSavedAmount(null);
        wish.setReactivatedCount(wish.getReactivatedCount() + 1);
        appendEvent(wish, WishEventType.REACTIVATED, now);

        return new WishStatusUpdateResponse(wish.getId(), wish.getStatus());
    }

    @Transactional
    public int expireDueWishes(LocalDateTime targetTime) {
        List<ProductWish> dueWishes = productWishRepository.findByStatusAndExpireAtLessThanEqual(
                WishStatus.WAITING,
                targetTime
        );

        for (ProductWish wish : dueWishes) {
            wish.setStatus(WishStatus.EXPIRED);
            wish.setExpiredAt(targetTime);
            wish.setSavedAmount(wish.effectivePrice());
            appendEvent(wish, WishEventType.EXPIRED, targetTime);
        }

        return dueWishes.size();
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
        if (currentStatus == WishStatus.EXPIRED || currentStatus == WishStatus.DELETED) {
            return;
        }

        throw new SavePocketException(
                ErrorCode.INVALID_WISH_STATE,
                "현재 상태(" + currentStatus + ")에서는 WAITING 처리할 수 없습니다."
        );
    }

    private void appendEvent(ProductWish wish, WishEventType eventType, LocalDateTime eventAt) {
        WishEventHistory event = new WishEventHistory();
        event.setWish(wish);
        event.setEventType(eventType);
        event.setEventAt(eventAt);
        wishEventHistoryRepository.save(event);
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
                product.image(),
                product.lowestPrice(),
                product.mallName()
        );
    }
}
