package io.github.mystagogy.savepocket.report.repository;

import java.time.LocalDateTime;

public interface MonthlySavingsDetailProjection {

    Long getWishId();

    String getWishName();

    LocalDateTime getOccurredAt();

    Long getAmount();
}
