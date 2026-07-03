package com.gmeo.finance_tracker.dashboard;

import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendResponse;
import com.gmeo.finance_tracker.dashboard.enums.DashboardTrendGroupBy;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return dashboardService.getSummary(fromDate, toDate);
    }

    @GetMapping("/category-breakdown")
    public DashboardCategoryBreakdownResponse getCategoryBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam TransactionType type) {
        return dashboardService.getCategoryBreakdown(fromDate, toDate, type);
    }

    @GetMapping("/trend")
    public DashboardTrendResponse getTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "MONTH") DashboardTrendGroupBy groupBy) {
        return dashboardService.getTrend(fromDate, toDate, groupBy);
    }
}
