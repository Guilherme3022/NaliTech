package com.nalitech.modules.parser.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Layout de importacao: mapeia os campos canonicos (data, valor, descricao,
 * documento) para os nomes das colunas do arquivo/planilha de origem.
 */
@Entity
@Table(name = "import_layouts")
@Getter
@Setter
@NoArgsConstructor
public class ImportLayout extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "col_data", length = 120)
    private String colData;

    @Column(name = "col_valor", length = 120)
    private String colValor;

    @Column(name = "col_descricao", length = 120)
    private String colDescricao;

    @Column(name = "col_documento", length = 120)
    private String colDocumento;

    @Column(nullable = false)
    private boolean ativo = true;

    // null = compartilhado (escritorio); preenchido = especifico do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
