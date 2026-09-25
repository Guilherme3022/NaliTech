package com.nalitech.modules.account.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.ai.AiSuggestionProvider.SuggestedAccount;
import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.modules.movement.entity.Movement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeuristicSuggestionProviderTest {

    @Mock
    private LearningHistoryRepository learningRepository;

    private HeuristicSuggestionProvider provider;

    private final UUID empresa = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        provider = new HeuristicSuggestionProvider(learningRepository);
    }

    private LearningHistory memoria(String padrao, UUID debito, UUID credito, int ocorrencias) {
        LearningHistory h = new LearningHistory();
        h.setEmpresaId(empresa);
        h.setClienteId(cliente);
        h.setDescricaoPadrao(padrao);
        h.setContaDebitoId(debito);
        h.setContaCreditoId(credito);
        h.setOcorrencias(ocorrencias);
        return h;
    }

    private Movement movimento(String descricao, String documento) {
        Movement m = new Movement();
        m.setId(UUID.randomUUID());
        m.setEmpresaId(empresa);
        m.setClienteId(cliente);
        m.setDescricao(descricao);
        m.setDocumento(documento);
        return m;
    }

    @Test
    void memoriaPorDescricaoDevolveOParDebitoCredito() {
        UUID debito = UUID.randomUUID();
        UUID credito = UUID.randomUUID();
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(memoria("black decker", debito, credito, 3)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("LIQUIDACAO BOLETO 53296273000191 BLACK DECKER", null), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaDebitoId()).isEqualTo(debito);
        assertThat(s.get().contaCreditoId()).isEqualTo(credito);
    }

    @Test
    void memoriaPorCnpjTemPrioridade() {
        UUID debito = UUID.randomUUID();
        UUID credito = UUID.randomUUID();
        when(learningRepository.findScoped(empresa, cliente, "#53296273000191"))
                .thenReturn(Optional.of(memoria("#53296273000191", debito, credito, 2)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PAGAMENTO FORNECEDOR", "53.296.273/0001-91"), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaDebitoId()).isEqualTo(debito);
        assertThat(s.get().contaCreditoId()).isEqualTo(credito);
        assertThat(s.get().confianca().intValue()).isEqualTo(85);
    }

    @Test
    void naoCasaSemTokenEmComum() {
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(memoria("energia eletrica", UUID.randomUUID(), null, 5)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PIX BLACK DECKER", null), List.of());

        assertThat(s).isEmpty();
    }

    @Test
    void desempataPelaMaiorOcorrencia() {
        UUID debitoA = UUID.randomUUID();
        UUID debitoB = UUID.randomUUID();
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(
                        memoria("black decker", debitoA, null, 2),
                        memoria("black decker", debitoB, null, 9)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("BLACK DECKER", null), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaDebitoId()).isEqualTo(debitoB);
    }
}
