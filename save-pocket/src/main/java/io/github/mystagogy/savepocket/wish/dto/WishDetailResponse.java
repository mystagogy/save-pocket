package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDateTime;
import java.util.List;

public record WishDetailResponse(
        Long id,
        String name,
        String productUrl,
        String imageUrl,
        String memo,
        Long referencePrice,
        Long userDealPrice,
        Long effectivePrice,
        WishStatus status,
        LocalDateTime lastViewedAt,
        LocalDateTime expireAt,
        Integer reactivatedCount,
        List<WishPriceHistoryItem> priceHistories,
        List<WishEventItem> events
) {
}
