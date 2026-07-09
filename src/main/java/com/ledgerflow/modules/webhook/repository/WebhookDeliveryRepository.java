package com.ledgerflow.modules.webhook.repository;

import com.ledgerflow.modules.webhook.entity.WebhookDelivery;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findBySubscriptionIdAndEmpresaId(UUID subscriptionId, UUID empresaId,
                                                           Pageable pageable);
}
