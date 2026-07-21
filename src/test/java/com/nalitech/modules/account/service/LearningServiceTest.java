package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private LearningHistoryRepository learningRepository;

    private LearningService service;
    private final UUID empresaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final UUID contaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LearningService(learningRepository);
    }

    @Test
    void documentoKeyNormalizaCnpjEIgnoraCurto() {
        assertThat(LearningService.documentoKey("92.559.830/0001-71")).isEqualTo("#92559830000171");
        assertThat(LearningService.documentoKey("123")).isNull();
        assertThat(LearningService.documentoKey(null)).isNull();
    }

    @Test
    void aprendePorNomeEPorCnpjQuandoHaDocumento() {
        when(learningRepository.findScoped(any(), any(), any())).thenReturn(Optional.empty());

        service.recordDecision(empresaId, clienteId, "PIX RECEBIDO GREEN CARD",
                "92.559.830/0001-71", contaId);

        ArgumentCaptor<LearningHistory> captor = ArgumentCaptor.forClass(LearningHistory.class);
        verify(learningRepository, atLeastOnce()).save(captor.capture());
        // Deve ter gravado uma chave por CNPJ (#...) e uma por nome (sem #).
        boolean temCnpj = captor.getAllValues().stream()
                .anyMatch(h -> h.getDescricaoPadrao().equals("#92559830000171"));
        boolean temNome = captor.getAllValues().stream()
                .anyMatch(h -> !h.getDescricaoPadrao().startsWith("#") && !h.getDescricaoPadrao().isBlank());
        assertThat(temCnpj).isTrue();
        assertThat(temNome).isTrue();
    }
}
