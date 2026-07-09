package com.ledgerflow.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        UUID empresaId,
        List<String> roles,
        UUID clienteId
) {
}
