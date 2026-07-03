package com.gmeo.finance_tracker.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownItem;
import com.gmeo.finance_tracker.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendItem;
import com.gmeo.finance_tracker.dashboard.dto.DashboardTrendResponse;
import com.gmeo.finance_tracker.dashboard.enums.DashboardTrendGroupBy;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
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

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getSummaryReturnsDashboardSummary() throws Exception {
        when(dashboardService.getSummary(null, null))
                .thenReturn(new DashboardSummaryResponse(
                        new BigDecimal("2500.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("2000.00"),
                        3));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(2500.00))
                .andExpect(jsonPath("$.totalExpense").value(500.00))
                .andExpect(jsonPath("$.balance").value(2000.00))
                .andExpect(jsonPath("$.transactionCount").value(3));
    }

    @Test
    void invalidDateRangeReturnsBadRequest() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 5, 31);
        when(dashboardService.getSummary(fromDate, toDate))
                .thenThrow(new BadRequestException("fromDate must be on or before toDate"));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("fromDate", "2026-06-01")
                        .param("toDate", "2026-05-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate must be on or before toDate"))
                .andExpect(jsonPath("$.path").value("/api/dashboard/summary"));
    }

    @Test
    void getCategoryBreakdownReturnsExpectedResponse() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        DashboardCategoryBreakdownItem item = new DashboardCategoryBreakdownItem(
                1L,
                "Food",
                TransactionType.EXPENSE,
                new BigDecimal("150.50"),
                3);
        when(dashboardService.getCategoryBreakdown(fromDate, toDate, TransactionType.EXPENSE))
                .thenReturn(new DashboardCategoryBreakdownResponse(
                        fromDate,
                        toDate,
                        TransactionType.EXPENSE,
                        new BigDecimal("150.50"),
                        3,
                        List.of(item)));

        mockMvc.perform(get("/api/dashboard/category-breakdown")
                        .param("fromDate", "2026-06-01")
                        .param("toDate", "2026-06-30")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromDate").value("2026-06-01"))
                .andExpect(jsonPath("$.toDate").value("2026-06-30"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.totalAmount").value(150.50))
                .andExpect(jsonPath("$.totalTransactionCount").value(3))
                .andExpect(jsonPath("$.items[0].categoryId").value(1))
                .andExpect(jsonPath("$.items[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.items[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.items[0].totalAmount").value(150.50))
                .andExpect(jsonPath("$.items[0].transactionCount").value(3));
    }

    @Test
    void getTrendReturnsExpectedResponse() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 2, 28);
        DashboardTrendItem item = new DashboardTrendItem(
                "2026-01",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                4);
        when(dashboardService.getTrend(fromDate, toDate, DashboardTrendGroupBy.MONTH))
                .thenReturn(new DashboardTrendResponse(
                        fromDate,
                        toDate,
                        DashboardTrendGroupBy.MONTH,
                        List.of(item)));

        mockMvc.perform(get("/api/dashboard/trend")
                        .param("fromDate", "2026-01-01")
                        .param("toDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromDate").value("2026-01-01"))
                .andExpect(jsonPath("$.toDate").value("2026-02-28"))
                .andExpect(jsonPath("$.groupBy").value("MONTH"))
                .andExpect(jsonPath("$.items[0].period").value("2026-01"))
                .andExpect(jsonPath("$.items[0].incomeAmount").value(1000.00))
                .andExpect(jsonPath("$.items[0].expenseAmount").value(500.00))
                .andExpect(jsonPath("$.items[0].balance").value(500.00))
                .andExpect(jsonPath("$.items[0].transactionCount").value(4));
    }
}
