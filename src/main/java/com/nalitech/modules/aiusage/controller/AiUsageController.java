package com.nalitech.modules.aiusage.controller;

import com.nalitech.modules.aiusage.dto.AiUsageDtos.AiUsageResponse;
import com.nalitech.modules.aiusage.service.AiUsageService;
import com.nalitech.security.SecurityUtils;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumo de IA da empresa autenticada (para conferir/faturar o custo real).
 * O painel cross-tenant (todas as empresas) fica no Prometheus/Grafana.
 */
@RestController
@RequestMapping("/ai-usage")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR')")
public class AiUsageController {

    private final AiUsageService aiUsageService;

    public AiUsageController(AiUsageService aiUsageService) {
        this.aiUsageService = aiUsageService;
    }

    // competencia opcional (YYYY-MM); sem ela, retorna todos os meses da empresa.
    @GetMapping
    public List<AiUsageResponse> usage(@RequestParam(required = false) String competencia) {
        return aiUsageService.usage(SecurityUtils.currentEmpresaId(), competencia);
    }
}
