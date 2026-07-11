package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.BankAccount;
import com.nalitech.modules.account.repository.BankAccountRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementType;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Monta o lancamento de partida dobrada de uma movimentacao a partir da conta
 * de contrapartida (resultado do De/Para) e da conta bancaria padrao.
 *
 * Convencao (extrato bancario):
 *   ENTRADA -> Debito = Banco,        Credito = Contrapartida (ex.: Receita)
 *   SAIDA   -> Debito = Contrapartida (ex.: Despesa), Credito = Banco
 *
 * Quando o tipo nao esta definido, usa o sinal do valor (>= 0 = entrada).
 * A conta bancaria e opcional: se nao houver banco padrao, so o lado da
 * contrapartida e preenchido (o contador ajusta o outro lado manualmente).
 */
@Service
public class DoubleEntryService {

    private final BankAccountRepository bankAccountRepository;

    public DoubleEntryService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void applyCounterpart(Movement movement, UUID contrapartidaContaId) {
        UUID contaBanco = resolveContaBanco(movement.getEmpresaId(), movement.getClienteId());
        if (isEntrada(movement)) {
            movement.setContaDebitoId(contaBanco);
            movement.setContaCreditoId(contrapartidaContaId);
        } else {
            movement.setContaDebitoId(contrapartidaContaId);
            movement.setContaCreditoId(contaBanco);
        }
    }

    private UUID resolveContaBanco(UUID empresaId, UUID clienteId) {
        // Banco padrao especifico do cliente tem prioridade; senao usa o compartilhado.
        return bankAccountRepository.findDefaultsApplicable(empresaId, clienteId).stream()
                .findFirst()
                .map(BankAccount::getContaContabilId)
                .orElse(null);
    }

    private boolean isEntrada(Movement movement) {
        if (movement.getTipo() != null) {
            return movement.getTipo() == MovementType.ENTRADA;
        }
        return movement.getValor() != null && movement.getValor().signum() >= 0;
    }
}
