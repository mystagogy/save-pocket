package io.github.mystagogy.savepocket.report.dto;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public record MonthlySavingsDetailItemResponse(
        Long wishId,
        String wishName,
        WishStatus status,
        LocalDateTime occurredAt,
        long signedAmount
) implements Serializable {
}
