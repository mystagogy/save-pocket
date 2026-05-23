package io.github.mystagogy.savepocket.notification.repository;

import io.github.mystagogy.savepocket.notification.entity.Notification;
import io.github.mystagogy.savepocket.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    boolean existsById(Long id);

    boolean existsByUser_IdAndWish_IdAndNotificationTypeAndCreatedAtGreaterThanEqual(
            Long userId,
            Long wishId,
            NotificationType notificationType,
            LocalDateTime createdAt
    );

    boolean existsByUser_IdAndNotificationTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            NotificationType notificationType,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByUser_IdAndReadFalse(Long userId);
}
