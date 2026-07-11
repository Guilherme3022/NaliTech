package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.entity.BankAccount;
import com.nalitech.modules.account.repository.BankAccountRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoubleEntryServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private DoubleEntryService service;

    private final UUID empresaId = UUID.randomUUID();
    private final UUID contaBanco = UUID.randomUUID();
    private final UUID contrapartida = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DoubleEntryService(bankAccountRepository);
    }

    private Movement movimento(MovementType tipo, BigDecimal valor) {
        Movement movement = new Movement();
        movement.setEmpresaId(empresaId);
        movement.setTipo(tipo);
        movement.setValor(valor);
        return movement;
    }

    private void comBancoPadrao() {
        BankAccount banco = new BankAccount();
        banco.setEmpresaId(empresaId);
        banco.setContaContabilId(contaBanco);
        banco.setPadrao(true);
        when(bankAccountRepository.findDefaultsApplicable(any(), any()))
                .thenReturn(List.of(banco));
    }

    @Test
    void entradaDebitaBancoECreditaContrapartida() {
        comBancoPadrao();
        Movement m = movimento(MovementType.ENTRADA, new BigDecimal("100.00"));

        service.applyCounterpart(m, contrapartida);

        assertThat(m.getContaDebitoId()).isEqualTo(contaBanco);
        assertThat(m.getContaCreditoId()).isEqualTo(contrapartida);
    }

    @Test
    void saidaDebitaContrapartidaECreditaBanco() {
        comBancoPadrao();
        Movement m = movimento(MovementType.SAIDA, new BigDecimal("100.00"));

        service.applyCounterpart(m, contrapartida);

        assertThat(m.getContaDebitoId()).isEqualTo(contrapartida);
        assertThat(m.getContaCreditoId()).isEqualTo(contaBanco);
    }

    @Test
    void semTipoUsaSinalDoValorNegativoComoSaida() {
        comBancoPadrao();
        Movement m = movimento(null, new BigDecimal("-50.00"));

        service.applyCounterpart(m, contrapartida);

        assertThat(m.getContaDebitoId()).isEqualTo(contrapartida);
        assertThat(m.getContaCreditoId()).isEqualTo(contaBanco);
    }

    @Test
    void semBancoPadraoPreencheSoOLadoDaContrapartida() {
        when(bankAccountRepository.findDefaultsApplicable(any(), any()))
                .thenReturn(List.of());
        Movement m = movimento(MovementType.SAIDA, new BigDecimal("100.00"));

        service.applyCounterpart(m, contrapartida);

        assertThat(m.getContaDebitoId()).isEqualTo(contrapartida);
        assertThat(m.getContaCreditoId()).isNull();
    }
}
