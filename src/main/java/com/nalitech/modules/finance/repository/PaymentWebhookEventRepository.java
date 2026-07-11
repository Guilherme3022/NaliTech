package com.nalitech.modules.finance.repository;

import com.nalitech.modules.finance.entity.PaymentWebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    boolean existsByProviderAndExternalIdAndEventType(String provider, String externalId, String eventType);
}
