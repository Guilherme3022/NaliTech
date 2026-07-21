package com.nalitech.modules.file.dto;

import com.nalitech.modules.file.entity.OrigemDocumento;
import com.nalitech.modules.file.entity.UploadStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class UploadDtos {

    private UploadDtos() {
    }

    public record UploadResponse(
            UUID id,
            UUID fileId,
            UUID clienteId,
            OrigemDocumento origem,
            UUID bankAccountId,
            String nomeOriginal,
            String tipoMime,
            long tamanho,
            UploadStatus status,
            String etapaAtual,
            String erroMensagem,
            OffsetDateTime createdAt) {
    }
}
