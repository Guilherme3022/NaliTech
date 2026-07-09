package com.ledgerflow.modules.file.event;

import java.util.UUID;

public record ArquivoRecebidoEvent(
        UUID uploadId,
        UUID empresaId,
        UUID fileId,
        UUID clienteId,
        String tipoMime,
        String nomeOriginal) {
}
