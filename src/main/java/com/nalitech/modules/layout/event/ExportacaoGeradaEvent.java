package com.nalitech.modules.layout.event;

import java.time.LocalDate;
import java.util.UUID;

public record ExportacaoGeradaEvent(
        UUID empresaId,
        String sistema,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        UUID fileId
) {
}
