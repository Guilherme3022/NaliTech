package com.nalitech.modules.apikey.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "chave_hash", nullable = false, unique = true, length = 120)
    private String chaveHash;

    @Column(length = 300)
    private String escopos;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_uso")
    private OffsetDateTime ultimoUso;
}
