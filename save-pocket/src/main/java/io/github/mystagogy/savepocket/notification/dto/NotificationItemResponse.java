package io.github.mystagogy.savepocket.notification.dto;

import io.github.mystagogy.savepocket.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationItemResponse(
        Long id,
        Long wishId,
        NotificationType notificationType,
        String title,
        String message,
        String linkUrl,
        boolean read,
        LocalDateTime createdAt
) {
}
