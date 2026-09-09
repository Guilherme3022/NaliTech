package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.account.entity.ChartAccountKind;
import com.nalitech.modules.account.service.ChartLayoutParser.ParsedAccount;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Cobre a separacao codigo reduzido x codigo de classificacao no layout de largura fixa
 * (arquivo com "codigo reduzido + codigo de classificacao" grudados na mesma coluna).
 */
class ChartLayoutParserCodigoTest {

    private final ChartLayoutParser parser = new ChartLayoutParser();

    private List<ParsedAccount> parse(String texto) {
        return parser.parse(texto.getBytes(StandardCharsets.UTF_8));
    }

    private ParsedAccount byNome(List<ParsedAccount> contas, String nome) {
        return contas.stream().filter(c -> c.nome().equals(nome)).findFirst().orElseThrow();
    }

    // 1 + 2 + 3 + 8: varios fornecedores com a MESMA classificacao (21301001) e reduzidos
    // diferentes; zeros a esquerda preservados; a flag "A" vira tipo e nao entra na descricao.
    @Test
    void separaReduzidoDaClassificacaoSemColisao() {
        String txt = """
                00000011                   ATIVO                                   S
                000000211                  ATIVO CIRCULANTE                        S
                000016521301               FORNECEDORES                            S
                000519821301001            ORCA DISTRIBUIDORA E TRANSPORTES LTDA   A
                000519921301001            BIRIBA INDUSTRIA DE BEBIDAS LTDA        A
                000520021301001            ITALIANY ALIMENTOS LTDA                 A
                """;

        List<ParsedAccount> contas = parse(txt);

        ParsedAccount orca = byNome(contas, "ORCA DISTRIBUIDORA E TRANSPORTES LTDA");
        assertThat(orca.codigo()).isEqualTo("0005198");               // reduzido (unico), com zeros
        assertThat(orca.codigoClassificacao()).isEqualTo("21301001");  // classificacao (repete)
        assertThat(orca.codigoOriginal()).isEqualTo("000519821301001"); // original completo
        assertThat(orca.kind()).isEqualTo(ChartAccountKind.ANALITICA);  // flag A -> tipo
        // A flag "A" virou tipo (kind) e NAO foi anexada ao nome.
        assertThat(orca.nome()).isEqualTo("ORCA DISTRIBUIDORA E TRANSPORTES LTDA");

        // Todos os fornecedores compartilham a classificacao...
        List<ParsedAccount> forn = contas.stream()
                .filter(c -> "21301001".equals(c.codigoClassificacao()))
                .toList();
        assertThat(forn).hasSize(3);
        assertThat(forn).allMatch(c -> c.kind() == ChartAccountKind.ANALITICA);
        // ...mas o identificador unico (reduzido) NAO colide.
        assertThat(forn.stream().map(ParsedAccount::codigo).distinct().toList())
                .containsExactlyInAnyOrder("0005198", "0005199", "0005200");
    }

    // Reduzido preserva zeros a esquerda e o original mantem a string integral. (A deteccao do
    // contador exige varias linhas sequenciais, entao usamos um trecho com algumas contas.)
    @Test
    void preservaZerosAEsquerda() {
        String txt = """
                000000411101               CAIXA                                   S
                000000511101001            CAIXA GERAL                             A
                000000611101001            FUNDO FIXO DE CAIXA                     A
                000000711102               BANCOS CONTA MOVIMENTO                  S
                """;
        ParsedAccount c = byNome(parse(txt), "CAIXA GERAL");
        assertThat(c.codigo()).isEqualTo("0000005");
        assertThat(c.codigoClassificacao()).isEqualTo("11101001");
        assertThat(c.codigoOriginal()).isEqualTo("000000511101001");
    }

    // 7: layout delimitado com codigo ja unico -> os tres codigos sao iguais (retrocompat).
    @Test
    void layoutDelimitadoMantemOsTresCodigosIguais() {
        String csv = """
                codigo,nome,tipo
                1.1.01.001,CAIXA GERAL,A
                1.1.02.001,BANCO X,A
                """;
        ParsedAccount c = byNome(parse(csv), "CAIXA GERAL");
        assertThat(c.codigo()).isEqualTo("1.1.01.001");
        assertThat(c.codigoClassificacao()).isEqualTo("1.1.01.001");
        assertThat(c.codigoOriginal()).isEqualTo("1.1.01.001");
    }
}
