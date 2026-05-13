package io.github.mystagogy.savepocket.report.controller;

import io.github.mystagogy.savepocket.common.response.ApiResponse;
import io.github.mystagogy.savepocket.common.security.CurrentUserProvider;
import io.github.mystagogy.savepocket.report.dto.MonthlySavingsResponse;
import io.github.mystagogy.savepocket.report.service.ReportService;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<MonthlySavingsResponse>> getMonthlySavings() {
        Long userId = currentUserProvider.getCurrentUserId();
        LocalDate now = LocalDate.now();
        MonthlySavingsResponse response = reportService.getMonthlySavings(userId, now.getYear(), now.getMonthValue());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
