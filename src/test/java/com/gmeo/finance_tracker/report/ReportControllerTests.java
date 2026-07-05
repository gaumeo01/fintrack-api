package com.gmeo.finance_tracker.report;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import com.gmeo.finance_tracker.report.dto.MonthlyReportResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class ReportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getMonthlyReportReturnsReport() throws Exception {
        MonthlyReportResponse response = new MonthlyReportResponse(
                "2026-06",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("1000.00"),
                new BigDecimal("400.00"),
                new BigDecimal("600.00"),
                5,
                List.of(),
                List.of(),
                List.of());
        when(reportService.getMonthlyReport("2026-06")).thenReturn(response);

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-06"))
                .andExpect(jsonPath("$.fromDate").value("2026-06-01"))
                .andExpect(jsonPath("$.toDate").value("2026-06-30"))
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpense").value(400.00))
                .andExpect(jsonPath("$.balance").value(600.00))
                .andExpect(jsonPath("$.transactionCount").value(5));

        verify(reportService).getMonthlyReport("2026-06");
    }

    @Test
    void getMonthlyReportReturnsBadRequestForInvalidMonth() throws Exception {
        when(reportService.getMonthlyReport("2026-13"))
                .thenThrow(new BadRequestException("month must use YYYY-MM format"));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month must use YYYY-MM format"));
    }

    @Test
    void getMonthlyReportReturnsBadRequestForMissingMonth() throws Exception {
        mockMvc.perform(get("/api/reports/monthly"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.month").exists());

        verifyNoInteractions(reportService);
    }
}
