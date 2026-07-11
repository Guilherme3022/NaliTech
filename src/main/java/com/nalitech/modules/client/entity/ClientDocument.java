package com.nalitech.modules.client.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_documents")
@Getter
@Setter
@NoArgsConstructor
public class ClientDocument extends TenantEntity {

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(length = 200)
    private String descricao;
}
