package com.nalitech.modules.account.controller;

import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationRequest;
import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationResponse;
import com.nalitech.modules.account.dto.AccountDtos.ParametrizationRequest;
import com.nalitech.modules.account.service.ParametrizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parametrization")
@PreAuthorize("hasAnyRole('ADMIN', 'CONTADOR', 'AUXILIAR')")
public class ParametrizationController {

    private final ParametrizationService parametrizationService;

    public ParametrizationController(ParametrizationService parametrizationService) {
        this.parametrizationService = parametrizationService;
    }

    /** Fila de solicitacao de parametrizacao: o que ainda falta mapear (De/Para). */
    @GetMapping("/requests")
    public List<ParametrizationRequest> requests() {
        return parametrizationService.pendingRequests();
    }

    /** Aplica um De/Para em lote (e, opcionalmente, cria a regra permanente). */
    @PostMapping("/apply")
    public ApplyParametrizationResponse apply(@Valid @RequestBody ApplyParametrizationRequest request) {
        return parametrizationService.apply(request);
    }
}
