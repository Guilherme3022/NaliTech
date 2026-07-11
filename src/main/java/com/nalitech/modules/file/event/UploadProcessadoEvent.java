package com.nalitech.modules.file.event;

import java.util.UUID;

public record UploadProcessadoEvent(
        UUID uploadId,
        UUID empresaId,
        int quantidadeMovimentacoes
) {
}
