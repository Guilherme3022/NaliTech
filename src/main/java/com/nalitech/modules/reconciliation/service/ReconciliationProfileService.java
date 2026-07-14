package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.reconciliation.dto.ReconciliationProfileDtos.ReconciliationProfileRequest;
import com.nalitech.modules.reconciliation.dto.ReconciliationProfileDtos.ReconciliationProfileResponse;
import com.nalitech.modules.reconciliation.entity.ReconciliationProfile;
import com.nalitech.modules.reconciliation.repository.ReconciliationProfileRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReconciliationProfileService {

    private final ReconciliationProfileRepository repository;
    private final ClientRepository clientRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    public ReconciliationProfileService(ReconciliationProfileRepository repository,
                                        ClientRepository clientRepository,
                                        ChartOfAccountRepository chartOfAccountRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationProfileResponse> list(UUID clienteId) {
        return repository.search(SecurityUtils.currentEmpresaId(), clienteId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationProfileResponse getById(UUID id) {
        return toResponse(find(id));
    }

    public ReconciliationProfileResponse create(ReconciliationProfileRequest request) {
        UUID empresaId = SecurityUtils.requireEmpresaId();
        clientRepository.findByIdAndEmpresaId(request.clienteId(), empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));
        // Etapa 7 da spec: o perfil precisa de um plano de contas para o cliente.
        if (!chartOfAccountRepository.existsPlanoForCliente(empresaId, request.clienteId())) {
            throw new BusinessException(
                    "Configure um plano de contas para o cliente antes de criar o perfil.",
                    HttpStatus.BAD_REQUEST);
        }
        ReconciliationProfile profile = new ReconciliationProfile();
        profile.setEmpresaId(empresaId);
        apply(profile, request);
        return toResponse(repository.save(profile));
    }

    public ReconciliationProfileResponse update(UUID id, ReconciliationProfileRequest request) {
        ReconciliationProfile profile = find(id);
        apply(profile, request);
        return toResponse(repository.save(profile));
    }

    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private void apply(ReconciliationProfile profile, ReconciliationProfileRequest request) {
        profile.setClienteId(request.clienteId());
        profile.setNome(request.nome());
        profile.setSistemaOrigem(request.sistemaOrigem());
        profile.setTipoArquivo(request.tipoArquivo());
        profile.setSistemaContabilDestino(request.sistemaContabilDestino());
        profile.setPlanoId(request.planoId());
        profile.setAtivo(request.ativo() == null || request.ativo());
    }

    private ReconciliationProfile find(UUID id) {
        return repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de conciliacao nao encontrado."));
    }

    private ReconciliationProfileResponse toResponse(ReconciliationProfile p) {
        return new ReconciliationProfileResponse(p.getId(), p.getClienteId(), p.getNome(),
                p.getSistemaOrigem(), p.getTipoArquivo(), p.getSistemaContabilDestino(),
                p.getPlanoId(), p.isAtivo());
    }
}
