package com.nalitech.modules.company.dto;

import com.nalitech.modules.company.entity.CompanyStatus;
import com.nalitech.shared.validation.Cnpj;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CreateCompanyRequest(
            @NotBlank @Cnpj String cnpj,
            @NotBlank String razaoSocial,
            String inscricaoEstadual,
            String regimeTributario,
            String plano) {
    }

    public record UpdateCompanyRequest(
            @NotBlank String razaoSocial,
            String inscricaoEstadual,
            String regimeTributario,
            String plano,
            CompanyStatus status,
            UUID responsavelId) {
    }

    public record CompanyResponse(
            UUID id,
            String cnpj,
            String razaoSocial,
            String inscricaoEstadual,
            String regimeTributario,
            String plano,
            String logoUrl,
            UUID responsavelId,
            CompanyStatus status) {
    }
}
