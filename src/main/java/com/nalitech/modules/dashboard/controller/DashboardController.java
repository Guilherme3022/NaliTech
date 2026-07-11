package com.nalitech.modules.dashboard.controller;

import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardActivity;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardPortfolio;
import com.nalitech.modules.dashboard.dto.DashboardDtos.DashboardSummary;
import com.nalitech.modules.dashboard.dto.DashboardDtos.OperationSummary;
import com.nalitech.modules.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/activity")
    public DashboardActivity activity() {
        return dashboardService.activity();
    }

    @GetMapping("/operation")
    public OperationSummary operation() {
        return dashboardService.operation();
    }

    @GetMapping("/portfolio")
    public DashboardPortfolio portfolio() {
        return dashboardService.portfolio();
    }
}
