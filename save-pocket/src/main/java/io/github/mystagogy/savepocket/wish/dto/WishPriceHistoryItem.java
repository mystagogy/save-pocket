package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.PriceType;
import java.time.LocalDateTime;

public record WishPriceHistoryItem(
        Long id,
        PriceType priceType,
        Long previousPrice,
        Long changedPrice,
        LocalDateTime changedAt
) {
}
