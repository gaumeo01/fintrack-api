package com.gmeo.finance_tracker.dashboard.dto;

import com.gmeo.finance_tracker.dashboard.enums.DashboardTrendGroupBy;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardTrendResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private DashboardTrendGroupBy groupBy;
    private List<DashboardTrendItem> items;
}
