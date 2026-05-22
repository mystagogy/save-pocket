package io.github.mystagogy.savepocket.wish.messaging;

import java.time.LocalDateTime;

public record PriceRefreshMessage(
        Long wishId,
        LocalDateTime requestedAt
) {
}
