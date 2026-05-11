package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;

public record WishCreateResponse(
        Long id,
        String name,
        String url,
        String imageUrl,
        Long referencePrice,
        Long userDealPrice,
        Long effectivePrice,
        WishStatus status,
        LocalDateTime lastViewedAt,
        LocalDateTime expireAt,
        Integer reactivatedCount
) {
}
