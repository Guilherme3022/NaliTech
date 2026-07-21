package com.nalitech.modules.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.file.entity.FileEntity;
import com.nalitech.modules.file.entity.Upload;
import com.nalitech.modules.file.repository.FileRepository;
import com.nalitech.modules.file.repository.UploadRepository;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.entity.Conciliacao;
import com.nalitech.modules.reconciliation.entity.ConciliacaoSituacao;
import com.nalitech.modules.reconciliation.repository.ConciliacaoRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationMatchRepository;
import com.nalitech.modules.reconciliation.repository.ReconciliationRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UploadServiceDeleteTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private UploadRepository uploadRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ConciliacaoRepository conciliacaoRepository;
    @Mock
    private MovementRepository movementRepository;
    @Mock
    private ReconciliationRepository reconciliationRepository;
    @Mock
    private ReconciliationMatchRepository matchRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final UUID empresaId = UUID.randomUUID();

    private UploadService service() {
        return new UploadService(fileRepository, uploadRepository, clientRepository,
                conciliacaoRepository, movementRepository, reconciliationRepository,
                matchRepository, storageService, eventPublisher);
    }

    private Upload upload(UUID fileId) {
        Upload upload = new Upload();
        upload.setId(UUID.randomUUID());
        upload.setEmpresaId(empresaId);
        upload.setFileId(fileId);
        return upload;
    }

    private FileEntity file(UUID id, String storageKey) {
        FileEntity file = new FileEntity();
        file.setId(id);
        file.setEmpresaId(empresaId);
        file.setStorageKey(storageKey);
        return file;
    }

    @Test
    void removeMovimentacoesStorageERegistroDeFileNaOrdemCerta() {
        UUID fileId = UUID.randomUUID();
        Upload upload = upload(fileId);
        FileEntity file = file(fileId, "empresa/arquivo.pdf");

        when(uploadRepository.findByIdAndEmpresaId(upload.getId(), empresaId))
                .thenReturn(Optional.of(upload));
        when(movementRepository.findByUploadId(upload.getId())).thenReturn(List.of());
        when(fileRepository.findByIdAndEmpresaId(fileId, empresaId)).thenReturn(Optional.of(file));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            service().delete(upload.getId());
        }

        // O binario no storage e o registro em `files` sao removidos...
        verify(storageService).delete("empresa/arquivo.pdf");
        verify(fileRepository).delete(file);

        // ...e o upload sai (com flush) ANTES do file, por causa da FK
        // uploads.file_id -> files ON DELETE CASCADE.
        InOrder ordem = inOrder(uploadRepository, fileRepository);
        ordem.verify(uploadRepository).delete(upload);
        ordem.verify(uploadRepository).flush();
        ordem.verify(fileRepository).delete(file);
    }

    @Test
    void naoExcluiArquivoDeConciliacaoConcluida() {
        UUID conciliacaoId = UUID.randomUUID();
        Upload upload = upload(UUID.randomUUID());
        upload.setConciliacaoId(conciliacaoId);

        Conciliacao conciliacao = new Conciliacao();
        conciliacao.setId(conciliacaoId);
        conciliacao.setEmpresaId(empresaId);
        conciliacao.setSituacao(ConciliacaoSituacao.CONCLUIDA);

        when(uploadRepository.findByIdAndEmpresaId(upload.getId(), empresaId))
                .thenReturn(Optional.of(upload));
        when(conciliacaoRepository.findByIdAndEmpresaId(conciliacaoId, empresaId))
                .thenReturn(Optional.of(conciliacao));

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            assertThatThrownBy(() -> service().delete(upload.getId()))
                    .isInstanceOf(BusinessException.class);
        }

        // Nada e apagado quando a conciliacao ja esta concluida (rastreabilidade).
        verify(uploadRepository, never()).delete(upload);
        verify(fileRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(storageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
