package io.github.mystagogy.savepocket.notification.messaging;

import java.time.LocalDateTime;

public record PriceDropNotificationMessage(
        Long userId,
        Long wishId,
        Long previousReferencePrice,
        Long latestReferencePrice,
        LocalDateTime occurredAt
) {
}
