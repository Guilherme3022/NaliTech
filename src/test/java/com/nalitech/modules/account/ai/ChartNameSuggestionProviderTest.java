package com.nalitech.modules.account.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.account.ai.AiSuggestionProvider.SuggestedAccount;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.movement.entity.Movement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChartNameSuggestionProviderTest {

    private final ChartNameSuggestionProvider provider = new ChartNameSuggestionProvider();

    private ChartOfAccount conta(String codigo, String nome) {
        ChartOfAccount c = new ChartOfAccount();
        c.setId(UUID.randomUUID());
        c.setCodigo(codigo);
        c.setNome(nome);
        return c;
    }

    private Movement movimento(String descricao) {
        Movement m = new Movement();
        m.setId(UUID.randomUUID());
        m.setEmpresaId(UUID.randomUUID());
        m.setClienteId(UUID.randomUUID());
        m.setDescricao(descricao);
        return m;
    }

    @Test
    void casaDescricaoComRuidoContraRazaoSocialDaConta() {
        // Caso real: "LIQUIDACAO BOLETO <cnpj> BLACK E DECKER" deve casar com a conta
        // "BLACK E DECKER DO BRASIL LTDA", apesar do ruido e das palavras extras.
        ChartOfAccount blackDecker = conta("0000605", "BLACK E DECKER DO BRASIL LTDA");
        List<ChartOfAccount> plano = List.of(
                conta("0000601", "NESTLE BRASIL LTDA"),
                blackDecker,
                conta("0000610", "ENERGIA ELETRICA"));

        Optional<SuggestedAccount> sugestao = provider.suggest(
                movimento("LIQUIDACAO BOLETO 53296273000191 BLACK E DECKER"), plano);

        assertThat(sugestao).isPresent();
        assertThat(sugestao.get().contaId()).isEqualTo(blackDecker.getId());
        assertThat(sugestao.get().confianca().intValue()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void casaContaDeTokenUnico() {
        ChartOfAccount nestle = conta("0000601", "NESTLE");
        Optional<SuggestedAccount> sugestao = provider.suggest(
                movimento("PIX RECEBIDO NESTLE"), List.of(nestle));

        assertThat(sugestao).isPresent();
        assertThat(sugestao.get().contaId()).isEqualTo(nestle.getId());
    }

    @Test
    void naoCasaApenasPorTermoGenericoOuGeografico() {
        // "brasil" e generico (stopword): nao deve casar so por causa dele.
        ChartOfAccount conta = conta("0000601", "BLACK E DECKER DO BRASIL LTDA");
        Optional<SuggestedAccount> sugestao = provider.suggest(
                movimento("DEPOSITO BRASIL"), List.of(conta));

        assertThat(sugestao).isEmpty();
    }

    @Test
    void semTokenEmComumNaoSugere() {
        ChartOfAccount conta = conta("0000610", "ENERGIA ELETRICA");
        Optional<SuggestedAccount> sugestao = provider.suggest(
                movimento("PAGAMENTO FORNECEDOR XPTO"), List.of(conta));

        assertThat(sugestao).isEmpty();
    }
}
