package com.ledgerflow.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends AuditableEntity {

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;
}
