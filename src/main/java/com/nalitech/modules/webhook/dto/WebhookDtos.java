package com.nalitech.modules.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class WebhookDtos {

    private WebhookDtos() {
    }

    public record CreateSubscriptionRequest(
            @NotBlank String evento,
            @NotBlank String urlDestino) {
    }

    public record SubscriptionResponse(
            UUID id, String evento, String urlDestino, boolean ativo, String segredo) {
    }
}
