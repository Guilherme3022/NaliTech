package com.nalitech.modules.file.event;

import java.util.UUID;

public record UploadErroEvent(
        UUID uploadId,
        UUID empresaId,
        String etapa,
        String mensagem
) {
}
