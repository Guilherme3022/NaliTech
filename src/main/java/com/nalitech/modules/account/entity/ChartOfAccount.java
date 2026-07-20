package com.nalitech.modules.account.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chart_of_accounts")
@Getter
@Setter
@NoArgsConstructor
public class ChartOfAccount extends TenantEntity {

    // Identificador UNICO da conta dentro do escopo (empresa + cliente). Para planos com
    // codigo reduzido, guarda o reduzido (ex.: "0005198"); para os demais, o proprio codigo.
    @Column(nullable = false, length = 30)
    private String codigo;

    // Codigo de CLASSIFICACAO (mascara hierarquica, ex.: "21301001"). PODE se repetir entre
    // contas distintas — usado apenas para hierarquia/agrupamento/relatorios, nunca como chave.
    @Column(name = "codigo_classificacao", length = 30)
    private String codigoClassificacao;

    // Codigo ORIGINAL completo, como veio no arquivo (ex.: "000519821301001"), sem perder zeros.
    @Column(name = "codigo_original", length = 60)
    private String codigoOriginal;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 20)
    private String tipo;

    // true = analitica (lancavel); false = sintetica (agrupadora); null = indefinida.
    @Column(name = "analitica")
    private Boolean analitica;

    // Natureza de saldo da conta: "DEVEDORA" / "CREDORA" / null (o que o D-/C- legado indicava).
    // Nao confundir com o lado do lancamento (partida dobrada), que e por movimentacao.
    @Column(name = "natureza_saldo", length = 10)
    private String naturezaSaldo;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "parent_id")
    private UUID parentId;

    // null = conta compartilhada (escritorio); preenchido = especifica do cliente.
    @Column(name = "cliente_id")
    private UUID clienteId;
}
