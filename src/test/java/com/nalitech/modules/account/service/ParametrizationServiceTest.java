package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationRequest;
import com.nalitech.modules.account.dto.AccountDtos.ApplyParametrizationResponse;
import com.nalitech.modules.account.entity.AccountRule;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParametrizationServiceTest {

    @Mock
    private MovementRepository movementRepository;
    @Mock
    private ClassificationService classificationService;
    @Mock
    private AccountRuleRepository accountRuleRepository;

    private final UUID empresaId = UUID.randomUUID();
    private final UUID contaId = UUID.randomUUID();

    private ParametrizationService service() {
        return new ParametrizationService(movementRepository, classificationService, accountRuleRepository);
    }

    private Movement movimento(String descricao) {
        Movement movement = new Movement();
        movement.setId(UUID.randomUUID());
        movement.setEmpresaId(empresaId);
        movement.setDescricao(descricao);
        movement.setStatus(MovementStatus.CONCILIADO);
        return movement;
    }

    @Test
    void aplicaDeParaEmLoteClassificandoCadaMovimentacao() {
        Movement m1 = movimento("PIX ENERGIA ELETRICA");
        Movement m2 = movimento("pix energia eletrica ref maio");
        when(movementRepository.findPendingByDescricaoContains(empresaId, MovementStatus.CONCILIADO, "energia"))
                .thenReturn(List.of(m1, m2));

        ApplyParametrizationResponse response;
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            response = service().apply(new ApplyParametrizationRequest("energia", contaId, false));
        }

        assertThat(response.classificados()).isEqualTo(2);
        assertThat(response.regraCriada()).isFalse();
        verify(classificationService).classify(m1.getId(), contaId);
        verify(classificationService).classify(m2.getId(), contaId);
        verify(accountRuleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void criaRegraPermanenteQuandoSolicitado() {
        when(movementRepository.findPendingByDescricaoContains(eq(empresaId), eq(MovementStatus.CONCILIADO),
                eq("aluguel"))).thenReturn(List.of(movimento("PAGAMENTO ALUGUEL")));

        ApplyParametrizationResponse response;
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            response = service().apply(new ApplyParametrizationRequest("aluguel", contaId, true));
        }

        assertThat(response.classificados()).isEqualTo(1);
        assertThat(response.regraCriada()).isTrue();

        ArgumentCaptor<AccountRule> captor = ArgumentCaptor.forClass(AccountRule.class);
        verify(accountRuleRepository).save(captor.capture());
        AccountRule salva = captor.getValue();
        assertThat(salva.getDescricaoContains()).isEqualTo("aluguel");
        assertThat(salva.getContaId()).isEqualTo(contaId);
        assertThat(salva.isAtivo()).isTrue();
    }

    @Test
    void agrupaPendentesPorDescricaoNormalizadaComContagem() {
        when(movementRepository.findByEmpresaIdAndStatusAndCategoriaSugeridaIsNull(
                empresaId, MovementStatus.CONCILIADO))
                .thenReturn(List.of(
                        movimento("TARIFA BANCARIA"),
                        movimento("tarifa   bancaria"),
                        movimento("PIX RECEBIDO")));

        List<?> requests;
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            requests = service().pendingRequests();
        }

        // "tarifa bancaria" (2 ocorrencias) + "pix recebido" (1) = 2 grupos
        assertThat(requests).hasSize(2);
    }
}
