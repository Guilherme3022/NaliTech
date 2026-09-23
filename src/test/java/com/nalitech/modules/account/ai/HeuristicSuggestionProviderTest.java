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

    private LearningHistory memoria(String padrao, UUID contaId, int ocorrencias) {
        LearningHistory h = new LearningHistory();
        h.setEmpresaId(empresa);
        h.setClienteId(cliente);
        h.setDescricaoPadrao(padrao);
        h.setContaId(contaId);
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
    void memoriaPorDescricaoCasaMesmoComRuido() {
        // Aprendido "black decker"; nova descricao vem com ruido (boleto/cnpj).
        UUID conta = UUID.randomUUID();
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(memoria("black decker", conta, 3)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("LIQUIDACAO BOLETO 53296273000191 BLACK DECKER", null), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaId()).isEqualTo(conta);
    }

    @Test
    void memoriaPorCnpjTemPrioridade() {
        UUID contaCnpj = UUID.randomUUID();
        // docKey do CNPJ 53.296.273/0001-91 -> "#53296273000191"
        when(learningRepository.findScoped(empresa, cliente, "#53296273000191"))
                .thenReturn(Optional.of(memoria("#53296273000191", contaCnpj, 2)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PAGAMENTO FORNECEDOR", "53.296.273/0001-91"), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaId()).isEqualTo(contaCnpj);
        // Confianca por CNPJ: min(95, 75 + ocorrencias*5) = 85.
        assertThat(s.get().confianca().intValue()).isEqualTo(85);
    }

    @Test
    void naoCasaPorTokenGenericoUnico() {
        // Memoria "energia eletrica" nao deve casar com "black decker" (0 tokens em comum).
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(memoria("energia eletrica", UUID.randomUUID(), 5)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("PIX BLACK DECKER", null), List.of());

        assertThat(s).isEmpty();
    }

    @Test
    void desempataPelaMaiorOcorrencia() {
        UUID contaA = UUID.randomUUID();
        UUID contaB = UUID.randomUUID();
        // Duas memorias com mesma sobreposicao (1.0); vence a de mais ocorrencias.
        when(learningRepository.findByScope(empresa, cliente))
                .thenReturn(List.of(
                        memoria("black decker", contaA, 2),
                        memoria("black decker", contaB, 9)));

        Optional<SuggestedAccount> s = provider.suggest(
                movimento("BLACK DECKER", null), List.of());

        assertThat(s).isPresent();
        assertThat(s.get().contaId()).isEqualTo(contaB);
    }
}
