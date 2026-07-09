package com.ledgerflow.modules.file.dto;

import com.ledgerflow.modules.file.entity.UploadStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class UploadDtos {

    private UploadDtos() {
    }

    public record UploadResponse(
            UUID id,
            UUID fileId,
            UUID clienteId,
            String nomeOriginal,
            String tipoMime,
            long tamanho,
            UploadStatus status,
            String etapaAtual,
            String erroMensagem,
            OffsetDateTime createdAt) {
    }
}
