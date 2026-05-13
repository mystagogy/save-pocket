package io.github.mystagogy.savepocket.report.dto;

import java.util.List;

public record MonthlySavingsResponse(
        int year,
        int month,
        long expiredAmount,
        long purchasedAmount,
        long netSavedAmount,
        List<MonthlySavingsDetailItemResponse> details
) {
}
