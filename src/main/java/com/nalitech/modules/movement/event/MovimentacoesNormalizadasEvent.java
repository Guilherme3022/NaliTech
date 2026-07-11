package com.nalitech.modules.movement.event;

import java.util.List;
import java.util.UUID;

public record MovimentacoesNormalizadasEvent(
        UUID uploadId,
        UUID empresaId,
        List<UUID> movementIds
) {
}
