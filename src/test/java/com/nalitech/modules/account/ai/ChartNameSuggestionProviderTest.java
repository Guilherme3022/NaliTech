package com.nalitech.modules.account.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.account.ai.AiSuggestionProvider.SuggestedAccount;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementType;
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

    private Movement movimento(String descricao, MovementType tipo) {
        Movement m = new Movement();
        m.setId(UUID.randomUUID());
        m.setEmpresaId(UUID.randomUUID());
        m.setClienteId(UUID.randomUUID());
        m.setDescricao(descricao);
        m.setTipo(tipo);
        return m;
    }

    @Test
    void saidaCasaContrapartidaNoDebito() {
        // "LIQUIDACAO BOLETO <cnpj> BLACK E DECKER" (SAIDA) -> contrapartida no DEBITO.
        ChartOfAccount blackDecker = conta("0000605", "BLACK E DECKER DO BRASIL LTDA");
        List<ChartOfAccount> plano = List.of(
                conta("0000601", "NESTLE BRASIL LTDA"),
                blackDecker,
                conta("0000610", "ENERGIA ELETRICA"));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("LIQUIDACAO BOLETO 53296273000191 BLACK E DECKER", MovementType.SAIDA), plano);

        assertThat(s).isPresent();
        assertThat(s.get().contaDebitoId()).isEqualTo(blackDecker.getId());
        assertThat(s.get().contaCreditoId()).isNull();
    }

    @Test
    void entradaCasaContrapartidaNoCredito() {
        ChartOfAccount nestle = conta("0000601", "NESTLE");
        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PIX RECEBIDO NESTLE", MovementType.ENTRADA), List.of(nestle));

        assertThat(s).isPresent();
        assertThat(s.get().contaCreditoId()).isEqualTo(nestle.getId());
        assertThat(s.get().contaDebitoId()).isNull();
    }

    @Test
    void casaPalavrasParecidas() {
        // "black deckers" (plural) deve casar com "BLACK E DECKER" (fuzzy por token).
        ChartOfAccount blackDecker = conta("0000605", "BLACK E DECKER DO BRASIL LTDA");
        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PAGAMENTO BLACK DECKERS", MovementType.SAIDA), List.of(blackDecker));

        assertThat(s).isPresent();
        assertThat(s.get().contaDebitoId()).isEqualTo(blackDecker.getId());
    }

    @Test
    void naoCasaApenasPorTermoGenericoOuGeografico() {
        ChartOfAccount conta = conta("0000601", "BLACK E DECKER DO BRASIL LTDA");
        Optional<SuggestedAccount> s = provider.suggest(
                movimento("DEPOSITO BRASIL", MovementType.SAIDA), List.of(conta));

        assertThat(s).isEmpty();
    }

    @Test
    void semTokenEmComumNaoSugere() {
        ChartOfAccount conta = conta("0000610", "ENERGIA ELETRICA");
        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PAGAMENTO FORNECEDOR XPTO", MovementType.SAIDA), List.of(conta));

        assertThat(s).isEmpty();
    }
}
