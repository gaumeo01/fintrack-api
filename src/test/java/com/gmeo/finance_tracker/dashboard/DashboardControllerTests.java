package com.gmeo.finance_tracker.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.common.exception.BadRequestException;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import com.gmeo.finance_tracker.dashboard.dto.DashboardSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
