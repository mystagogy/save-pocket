package io.github.mystagogy.savepocket.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import io.github.mystagogy.savepocket.notification.dto.NotificationListResponse;
import io.github.mystagogy.savepocket.notification.dto.NotificationReadResponse;
import io.github.mystagogy.savepocket.notification.entity.Notification;
import io.github.mystagogy.savepocket.notification.entity.NotificationType;
import io.github.mystagogy.savepocket.notification.repository.NotificationRepository;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProductWishRepository productWishRepository;

    @Mock
    private NotificationSseService notificationSseService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                productWishRepository,
                notificationSseService
        );
    }

    @Test
    void getNotificationsReturnsUnreadCountAndRecentItems() {
        Notification notification = createNotification(1L, 11L);
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(List.of(notification));
        when(notificationRepository.countByUser_IdAndReadFalse(1L)).thenReturn(3L);

        NotificationListResponse response = notificationService.getNotifications(1L, 10);

        assertThat(response.unreadCount()).isEqualTo(3L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).wishId()).isEqualTo(11L);
    }

    @Test
    void markAsReadThrowsNotFoundWhenNotificationMissing() {
        when(notificationRepository.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.empty());
        when(notificationRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 7L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void createPriceDropLowestNotificationSkipsWhenDuplicatedWithin24Hours() {
        ProductWish wish = createWish(1L, 10L, "테스트 상품");
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 21, 10, 0);

        when(productWishRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(wish));
        when(notificationRepository.existsByUser_IdAndWish_IdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                1L,
                10L,
                NotificationType.PRICE_DROP_LOWEST,
                occurredAt.minusHours(24)
        )).thenReturn(true);

        notificationService.createPriceDropLowestNotification(1L, 10L, 130000L, 120000L, occurredAt);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationSseService, never()).publishNotification(any(), any());
    }

    @Test
    void createPriceDropLowestNotificationSavesAndPublishesSse() {
        ProductWish wish = createWish(1L, 20L, "에어팟");
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 21, 11, 0);

        when(productWishRepository.findByIdAndUser_Id(20L, 1L)).thenReturn(Optional.of(wish));
        when(notificationRepository.existsByUser_IdAndWish_IdAndNotificationTypeAndCreatedAtGreaterThanEqual(
                1L,
                20L,
                NotificationType.PRICE_DROP_LOWEST,
                occurredAt.minusHours(24)
        )).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            ReflectionTestUtils.setField(saved, "createdAt", occurredAt);
            return saved;
        });

        notificationService.createPriceDropLowestNotification(1L, 20L, 199000L, 189000L, occurredAt);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getNotificationType()).isEqualTo(NotificationType.PRICE_DROP_LOWEST);
        assertThat(notificationCaptor.getValue().getMessage()).contains("199000원").contains("189000원");

        verify(notificationSseService).publishNotification(eq(1L), any());
    }

    private Notification createNotification(Long userId, Long wishId) {
        Notification notification = new Notification();
        notification.setNotificationType(NotificationType.PRICE_DROP_LOWEST);
        notification.setTitle("최저가 갱신");
        notification.setMessage("가격이 내려갔어요.");
        notification.setLinkUrl("/wishes/" + wishId);
        ReflectionTestUtils.setField(notification, "id", 1L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 5, 21, 12, 0));

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        notification.setUser(user);

        ProductWish wish = new ProductWish();
        ReflectionTestUtils.setField(wish, "id", wishId);
        notification.setWish(wish);
        return notification;
    }

    private ProductWish createWish(Long userId, Long wishId, String productName) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded");
        user.setNickname("tester");

        ProductWish wish = new ProductWish();
        ReflectionTestUtils.setField(wish, "id", wishId);
        wish.setUser(user);
        wish.setProductName(productName);
        wish.setProductUrl("https://example.com/" + wishId);
        return wish;
    }
}
