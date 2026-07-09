package com.ledgerflow.modules.webhook.repository;

import com.ledgerflow.modules.webhook.entity.WebhookSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    List<WebhookSubscription> findByEmpresaIdAndEventoAndAtivoTrue(UUID empresaId, String evento);

    List<WebhookSubscription> findByEmpresaId(UUID empresaId);

    Optional<WebhookSubscription> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
