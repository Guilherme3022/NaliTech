package com.ledgerflow.modules.dashboard.controller;

import com.ledgerflow.modules.dashboard.dto.DashboardDtos.DashboardActivity;
import com.ledgerflow.modules.dashboard.dto.DashboardDtos.DashboardSummary;
import com.ledgerflow.modules.dashboard.service.DashboardService;
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
}
