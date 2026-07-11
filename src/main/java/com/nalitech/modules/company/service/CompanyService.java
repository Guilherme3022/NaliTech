package com.nalitech.modules.company.service;

import com.nalitech.modules.company.dto.CompanyDtos.CompanyResponse;
import com.nalitech.modules.company.dto.CompanyDtos.CreateCompanyRequest;
import com.nalitech.modules.company.dto.CompanyDtos.UpdateCompanyRequest;
import com.nalitech.modules.company.entity.Company;
import com.nalitech.modules.company.mapper.CompanyMapper;
import com.nalitech.modules.company.repository.CompanyRepository;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import com.nalitech.shared.validation.CnpjValidator;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyService(CompanyRepository companyRepository, CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
    }

    public CompanyResponse create(CreateCompanyRequest request) {
        String cnpj = CnpjValidator.normalize(request.cnpj());
        if (companyRepository.existsByCnpj(cnpj)) {
            throw new BusinessException("Ja existe empresa com este CNPJ.", HttpStatus.CONFLICT);
        }
        Company company = new Company();
        company.setCnpj(cnpj);
        company.setRazaoSocial(request.razaoSocial());
        company.setInscricaoEstadual(request.inscricaoEstadual());
        company.setRegimeTributario(request.regimeTributario());
        company.setPlano(request.plano());
        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> list(Pageable pageable) {
        return companyRepository.findAll(pageable).map(companyMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID id) {
        return companyMapper.toResponse(findById(id));
    }

    public CompanyResponse update(UUID id, UpdateCompanyRequest request) {
        Company company = findById(id);
        company.setRazaoSocial(request.razaoSocial());
        company.setInscricaoEstadual(request.inscricaoEstadual());
        company.setRegimeTributario(request.regimeTributario());
        company.setPlano(request.plano());
        company.setResponsavelId(request.responsavelId());
        if (request.status() != null) {
            company.setStatus(request.status());
        }
        return companyMapper.toResponse(companyRepository.save(company));
    }

    public void updateLogo(UUID id, String logoUrl) {
        Company company = findById(id);
        company.setLogoUrl(logoUrl);
        companyRepository.save(company);
    }

    private Company findById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa nao encontrada."));
    }
}
