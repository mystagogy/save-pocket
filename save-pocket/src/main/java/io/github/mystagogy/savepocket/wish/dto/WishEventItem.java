package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.WishEventType;
import java.time.LocalDateTime;

public record WishEventItem(
        Long id,
        WishEventType eventType,
        LocalDateTime eventAt,
        String description,
        String metadata
) {
}
