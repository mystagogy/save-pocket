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
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDateTime;
import java.util.List;
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
}
