package com.nalitech.modules.reconciliation.service;

import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.file.entity.Upload;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.modules.reconciliation.dto.ConciliacaoDtos.ConciliacaoResponse;
import com.nalitech.modules.reconciliation.entity.Conciliacao;
import com.nalitech.modules.reconciliation.entity.ConciliacaoSituacao;
import com.nalitech.modules.reconciliation.repository.ConciliacaoRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationProfileRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConciliacaoService {

    private final ConciliacaoRepository conciliacaoRepository;
    private final ClientRepository clientRepository;
    private final UploadRepository uploadRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final ReconciliationProfileRepository profileRepository;

    public ConciliacaoService(ConciliacaoRepository conciliacaoRepository,
                              ClientRepository clientRepository,
                              UploadRepository uploadRepository,
                              ChartOfAccountRepository chartOfAccountRepository,
                              ReconciliationProfileRepository profileRepository) {
        this.conciliacaoRepository = conciliacaoRepository;
        this.clientRepository = clientRepository;
        this.uploadRepository = uploadRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<ConciliacaoResponse> list(UUID clienteId, LocalDate competencia) {
        return conciliacaoRepository
                .search(SecurityUtils.currentEmpresaId(), clienteId, competencia)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ConciliacaoResponse create(UUID clienteId, LocalDate competencia, UUID perfilId) {
        UUID empresaId = SecurityUtils.requireEmpresaId();
        // Regra: nenhuma conciliacao sem cliente (spec item 5/12).
        clientRepository.findByIdAndEmpresaId(clienteId, empresaId)
                .orElseThrow(() -> new BusinessException(
                        "Cliente invalido para esta empresa.", HttpStatus.BAD_REQUEST));

        // Se um perfil foi escolhido, valida que pertence a empresa e ao cliente.
        if (perfilId != null) {
            var perfil = profileRepository.findByIdAndEmpresaId(perfilId, empresaId)
                    .orElseThrow(() -> new BusinessException(
                            "Perfil de conciliacao invalido.", HttpStatus.BAD_REQUEST));
            if (!perfil.getClienteId().equals(clienteId)) {
                throw new BusinessException(
                        "O perfil escolhido e de outro cliente.", HttpStatus.BAD_REQUEST);
            }
        }

        Conciliacao conciliacao = new Conciliacao();
        conciliacao.setEmpresaId(empresaId);
        conciliacao.setClienteId(clienteId);
        conciliacao.setCompetencia(competencia);
        conciliacao.setPerfilId(perfilId);
        conciliacao.setSituacao(ConciliacaoSituacao.RASCUNHO);
        return toResponse(conciliacaoRepository.save(conciliacao));
    }

    @Transactional(readOnly = true)
    public ConciliacaoResponse getById(UUID id) {
        return toResponse(findInCurrentCompany(id));
    }

    /** Anexa um upload ao lote (ED). O upload precisa ser do mesmo cliente. */
    public ConciliacaoResponse attachUpload(UUID id, UUID uploadId) {
        Conciliacao conciliacao = findInCurrentCompany(id);
        garantirEditavel(conciliacao);
        Upload upload = uploadRepository
                .findByIdAndEmpresaId(uploadId, conciliacao.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Upload nao encontrado."));
        if (upload.getClienteId() == null || !upload.getClienteId().equals(conciliacao.getClienteId())) {
            throw new BusinessException(
                    "O arquivo pertence a outro cliente.", HttpStatus.BAD_REQUEST);
        }
        upload.setConciliacaoId(conciliacao.getId());
        uploadRepository.save(upload);
        conciliacao.setSituacao(ConciliacaoSituacao.AGUARDANDO_PARAMETRIZACAO);
        return toResponse(conciliacaoRepository.save(conciliacao));
    }

    /** Conclui o lote (ED). Exige plano de contas ativo e ao menos um arquivo. */
    public ConciliacaoResponse concluir(UUID id) {
        Conciliacao conciliacao = findInCurrentCompany(id);
        garantirEditavel(conciliacao);
        if (uploadRepository.countByConciliacaoId(conciliacao.getId()) == 0) {
            throw new BusinessException(
                    "Anexe pelo menos um arquivo antes de concluir a conciliacao.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!chartOfAccountRepository.existsPlanoForCliente(
                conciliacao.getEmpresaId(), conciliacao.getClienteId())) {
            throw new BusinessException(
                    "Nao foi identificado um plano de contas ativo para este cliente. "
                    + "Configure ou vincule um plano de contas antes de concluir a conciliacao.",
                    HttpStatus.BAD_REQUEST);
        }
        conciliacao.setSituacao(ConciliacaoSituacao.CONCLUIDA);
        return toResponse(conciliacaoRepository.save(conciliacao));
    }

    /** Cancela o lote (ED). */
    public ConciliacaoResponse cancelar(UUID id) {
        Conciliacao conciliacao = findInCurrentCompany(id);
        if (conciliacao.getSituacao() == ConciliacaoSituacao.CONCLUIDA) {
            throw new BusinessException(
                    "Uma conciliacao concluida nao pode ser cancelada.", HttpStatus.BAD_REQUEST);
        }
        conciliacao.setSituacao(ConciliacaoSituacao.CANCELADA);
        return toResponse(conciliacaoRepository.save(conciliacao));
    }

    @Transactional(readOnly = true)
    public Conciliacao requireConcluida(UUID id) {
        Conciliacao conciliacao = findInCurrentCompany(id);
        if (conciliacao.getSituacao() != ConciliacaoSituacao.CONCLUIDA) {
            throw new BusinessException(
                    "O download so fica disponivel apos concluir a conciliacao.",
                    HttpStatus.BAD_REQUEST);
        }
        return conciliacao;
    }

    private void garantirEditavel(Conciliacao conciliacao) {
        if (conciliacao.getSituacao() == ConciliacaoSituacao.CONCLUIDA
                || conciliacao.getSituacao() == ConciliacaoSituacao.CANCELADA) {
            throw new BusinessException(
                    "Esta conciliacao esta " + conciliacao.getSituacao() + " e nao pode ser alterada.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Conciliacao findInCurrentCompany(UUID id) {
        return conciliacaoRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conciliacao nao encontrada."));
    }

    // Uploads ainda em andamento (pipeline OCR/parse/normalizacao nao finalizado).
    private static final java.util.List<com.nalitech.modules.file.entity.UploadStatus>
            UPLOAD_EM_PROCESSAMENTO = java.util.List.of(
                    com.nalitech.modules.file.entity.UploadStatus.RECEBIDO,
                    com.nalitech.modules.file.entity.UploadStatus.VALIDANDO,
                    com.nalitech.modules.file.entity.UploadStatus.PROCESSANDO);

    private ConciliacaoResponse toResponse(Conciliacao c) {
        boolean processando = uploadRepository
                .countByConciliacaoIdAndStatusIn(c.getId(), UPLOAD_EM_PROCESSAMENTO) > 0;
        return new ConciliacaoResponse(c.getId(), c.getClienteId(), c.getCompetencia(),
                c.getPerfilId(), c.getSituacao(), processando);
    }
}
