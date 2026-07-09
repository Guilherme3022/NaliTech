package com.ledgerflow.modules.file.entity;

import com.ledgerflow.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "uploads")
@Getter
@Setter
@NoArgsConstructor
public class Upload extends TenantEntity {

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadStatus status = UploadStatus.RECEBIDO;

    @Column(name = "etapa_atual", length = 40)
    private String etapaAtual;

    @Column(name = "erro_mensagem", length = 500)
    private String erroMensagem;

    public void marcarErro(String etapa, String mensagem) {
        this.status = UploadStatus.ERRO;
        this.etapaAtual = etapa;
        this.erroMensagem = mensagem;
    }

    public void avancar(UploadStatus novoStatus, String etapa) {
        this.status = novoStatus;
        this.etapaAtual = etapa;
    }
}
