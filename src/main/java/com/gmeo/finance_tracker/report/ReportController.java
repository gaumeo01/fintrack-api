package com.gmeo.finance_tracker.report;

import com.gmeo.finance_tracker.report.dto.MonthlyReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public MonthlyReportResponse getMonthlyReport(@RequestParam String month) {
        return reportService.getMonthlyReport(month);
    }
}
