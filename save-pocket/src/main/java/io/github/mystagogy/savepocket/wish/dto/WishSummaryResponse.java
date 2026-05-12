package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;

public record WishSummaryResponse(
        Long id,
        String name,
        String imageUrl,
        WishStatus status,
        Long effectivePrice,
        LocalDateTime expireAt
) {
}
