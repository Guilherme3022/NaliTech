package com.nalitech.modules.file.entity;

import com.nalitech.shared.domain.TenantEntity;
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

    // Papel do documento (EXTRATO x SISTEMA) para o matching extrato x sistema.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrigemDocumento origem = OrigemDocumento.EXTRATO;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    // Lote (Conciliacao) ao qual o arquivo foi anexado (ED).
    @Column(name = "conciliacao_id")
    private UUID conciliacaoId;

    // Versionamento/substituicao (EE - spec secoes 17-18).
    @Column(nullable = false)
    private int versao = 1;

    @Column(name = "substituido_por_id")
    private UUID substituidoPorId;

    @Column(name = "justificativa_substituicao", length = 500)
    private String justificativaSubstituicao;

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
