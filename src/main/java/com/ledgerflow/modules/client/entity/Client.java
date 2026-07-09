package com.ledgerflow.modules.client.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import com.ledgerflow.shared.security.AttributeEncryptor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client extends TenantEntity {

    @Column(nullable = false, length = 180)
    private String nome;

    @Column(name = "cnpj_cpf", nullable = false, length = 14)
    private String cnpjCpf;

    @Column(length = 120)
    private String contato;

    @Convert(converter = AttributeEncryptor.class)
    @Column(length = 200)
    private String telefone;

    @Column(length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ATIVO;
}
