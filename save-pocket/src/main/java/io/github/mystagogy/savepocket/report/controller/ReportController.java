package io.github.mystagogy.savepocket.report.controller;

import io.github.mystagogy.savepocket.common.response.ApiResponse;
import io.github.mystagogy.savepocket.common.security.CurrentUserProvider;
import io.github.mystagogy.savepocket.report.dto.MonthlySavingsResponse;
import io.github.mystagogy.savepocket.report.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    public ReportController(ReportService reportService, CurrentUserProvider currentUserProvider) {
        this.reportService = reportService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlySavingsResponse>> getMonthlySavings(
            @RequestParam(required = false) @Min(2000) @Max(9999) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        Long userId = currentUserProvider.getCurrentUserId();
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        MonthlySavingsResponse response = reportService.getMonthlySavings(userId, targetYear, targetMonth);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
