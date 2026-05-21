package io.github.mystagogy.savepocket.notification.dto;

import java.util.List;

public record NotificationListResponse(
        long unreadCount,
        List<NotificationItemResponse> items
) {
}
