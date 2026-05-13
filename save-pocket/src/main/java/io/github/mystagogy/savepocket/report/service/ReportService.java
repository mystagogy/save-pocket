package io.github.mystagogy.savepocket.report.service;

import io.github.mystagogy.savepocket.report.dto.MonthlySavingsDetailItemResponse;
import io.github.mystagogy.savepocket.report.dto.MonthlySavingsResponse;
import io.github.mystagogy.savepocket.report.repository.MonthlySavingsDetailProjection;
import io.github.mystagogy.savepocket.report.repository.ReportRepository;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySavingsResponse getMonthlySavings(Long userId, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime end = monthStart.plusMonths(1).atStartOfDay();

        long expiredAmount = reportRepository.sumExpiredAmountByUserAndPeriod(
                userId,
                start,
                end
        );
        long purchasedAmount = reportRepository.sumPurchasedAmountByUserAndPeriod(
                userId,
                WishStatus.PURCHASED,
                start,
                end
        );
        List<MonthlySavingsDetailItemResponse> details = toDetailItems(
                reportRepository.findExpiredDetailsByUserAndPeriod(userId, start, end),
                reportRepository.findPurchasedDetailsByUserAndPeriod(userId, WishStatus.PURCHASED, start, end)
        );

        return new MonthlySavingsResponse(
                year,
                month,
                expiredAmount,
                purchasedAmount,
                expiredAmount - purchasedAmount,
                details
        );
    }

    private List<MonthlySavingsDetailItemResponse> toDetailItems(
            List<MonthlySavingsDetailProjection> expiredDetails,
            List<MonthlySavingsDetailProjection> purchasedDetails
    ) {
        List<MonthlySavingsDetailItemResponse> items = new ArrayList<>();

        expiredDetails.stream()
                .map(detail -> new MonthlySavingsDetailItemResponse(
                    detail.getWishId(),
                    detail.getWishName(),
                    WishStatus.EXPIRED,
                    detail.getOccurredAt(),
                    detail.getAmount()
                ))
                .forEach(items::add);

        purchasedDetails.stream()
                .map(detail -> new MonthlySavingsDetailItemResponse(
                    detail.getWishId(),
                    detail.getWishName(),
                    WishStatus.PURCHASED,
                    detail.getOccurredAt(),
                    -detail.getAmount()
                ))
                .forEach(items::add);

        items.sort(Comparator.comparing(MonthlySavingsDetailItemResponse::occurredAt).reversed());
        return items;
    }
}
