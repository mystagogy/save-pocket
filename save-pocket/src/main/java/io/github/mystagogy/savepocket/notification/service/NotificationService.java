package io.github.mystagogy.savepocket.notification.service;

import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import io.github.mystagogy.savepocket.notification.dto.NotificationItemResponse;
import io.github.mystagogy.savepocket.notification.dto.NotificationListResponse;
import io.github.mystagogy.savepocket.notification.dto.NotificationReadResponse;
import io.github.mystagogy.savepocket.notification.entity.Notification;
import io.github.mystagogy.savepocket.notification.entity.NotificationType;
import io.github.mystagogy.savepocket.notification.repository.NotificationRepository;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final long DUPLICATION_COOLDOWN_HOURS = 24L;

    private final NotificationRepository notificationRepository;
    private final ProductWishRepository productWishRepository;
    private final NotificationSseService notificationSseService;

    public NotificationService(
            NotificationRepository notificationRepository,
            ProductWishRepository productWishRepository,
            NotificationSseService notificationSseService
    ) {
        this.notificationRepository = notificationRepository;
        this.productWishRepository = productWishRepository;
        this.notificationSseService = notificationSseService;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, Integer limit) {
        int resolvedLimit = resolveLimit(limit);
        List<NotificationItemResponse> items = notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, resolvedLimit))
                .stream()
                .map(this::toItem)
                .toList();
        long unreadCount = notificationRepository.countByUser_IdAndReadFalse(userId);
        return new NotificationListResponse(unreadCount, items);
    }

    @Transactional
    public NotificationReadResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> {
                    if (notificationRepository.existsById(notificationId)) {
                        return new SavePocketException(ErrorCode.FORBIDDEN_RESOURCE);
                    }
                    return new SavePocketException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        if (!notification.isRead()) {
            notification.markAsRead(LocalDateTime.now());
        }
        return new NotificationReadResponse(notification.getId(), true);
    }

    @Transactional
    public void createPriceDropLowestNotification(
            Long userId,
            Long wishId,
            Long previousReferencePrice,
            Long latestReferencePrice,
            LocalDateTime occurredAt
    ) {
        ProductWish wish = productWishRepository.findByIdAndUser_Id(wishId, userId)
                .orElseThrow(() -> new SavePocketException(ErrorCode.WISH_NOT_FOUND));

        LocalDateTime threshold = occurredAt.minusHours(DUPLICATION_COOLDOWN_HOURS);
        boolean duplicated = notificationRepository.existsByUser_IdAndWish_IdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                userId,
                wishId,
                NotificationType.PRICE_DROP_LOWEST,
                threshold
        );
        if (duplicated) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(wish.getUser());
        notification.setWish(wish);
        notification.setNotificationType(NotificationType.PRICE_DROP_LOWEST);
        notification.setTitle("최저가 갱신");
        notification.setMessage(wish.getProductName() + " 최저가가 " + previousReferencePrice + "원에서 "
                + latestReferencePrice + "원으로 내려갔어요.");
        notification.setLinkUrl("/wishes/" + wish.getId());
        Notification saved = notificationRepository.save(notification);

        notificationSseService.publishNotification(userId, toItem(saved));
    }

    @Transactional
    public DailySavedSummaryResult createDailySavedSummaryNotifications(LocalDate targetDate) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<ProductWishRepository.DailySavedAmountSummary> summaries =
                productWishRepository.summarizeDailySavedAmountByUser(WishStatus.EXPIRED, start, end);

        int scannedCount = summaries.size();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ProductWishRepository.DailySavedAmountSummary summary : summaries) {
            try {
                Long userId = summary.getUserId();
                Long totalSavedAmount = summary.getTotalSavedAmount();
                Long representativeWishId = summary.getRepresentativeWishId();
                if (userId == null || totalSavedAmount == null || totalSavedAmount <= 0 || representativeWishId == null) {
                    skippedCount++;
                    continue;
                }

                boolean duplicated =
                        notificationRepository.existsByUser_IdAndNotificationTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                userId,
                                NotificationType.DAILY_SAVED_SUMMARY,
                                start,
                                end
                        );
                if (duplicated) {
                    skippedCount++;
                    continue;
                }

                ProductWish representativeWish = productWishRepository.findByIdAndUser_Id(representativeWishId, userId)
                        .orElse(null);
                if (representativeWish == null) {
                    skippedCount++;
                    continue;
                }

                Notification notification = new Notification();
                notification.setUser(representativeWish.getUser());
                notification.setWish(representativeWish);
                notification.setNotificationType(NotificationType.DAILY_SAVED_SUMMARY);
                notification.setTitle("오늘의 절약 리포트");
                notification.setMessage("오늘도 총 " + formatCurrency(totalSavedAmount) + "원을 아끼셨어요.");
                notification.setLinkUrl("/reports/monthly");
                Notification saved = notificationRepository.save(notification);
                notificationSseService.publishNotification(userId, toItem(saved));
                createdCount++;
            } catch (RuntimeException ex) {
                failedCount++;
            }
        }

        return new DailySavedSummaryResult(scannedCount, createdCount, skippedCount, failedCount);
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private NotificationItemResponse toItem(Notification notification) {
        Long wishId = notification.getWish() != null ? notification.getWish().getId() : null;
        String linkUrl = StringUtils.hasText(notification.getLinkUrl()) ? notification.getLinkUrl() : null;
        return new NotificationItemResponse(
                notification.getId(),
                wishId,
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                linkUrl,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private String formatCurrency(Long amount) {
        if (amount == null) {
            return "0";
        }
        return String.format(Locale.KOREA, "%,d", amount);
    }

    public record DailySavedSummaryResult(
            int scannedCount,
            int createdCount,
            int skippedCount,
            int failedCount
    ) {
    }
}
