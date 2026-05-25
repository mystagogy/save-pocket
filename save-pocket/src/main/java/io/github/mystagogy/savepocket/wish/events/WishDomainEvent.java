package io.github.mystagogy.savepocket.wish.events;

import java.time.LocalDateTime;

public record WishDomainEvent(
        String eventId,
        WishDomainEventType eventType,
        int schemaVersion,
        LocalDateTime occurredAt,
        Long wishId,
        Long userId,
        String status,
        Long previousReferencePrice,
        Long currentReferencePrice
) {
}
