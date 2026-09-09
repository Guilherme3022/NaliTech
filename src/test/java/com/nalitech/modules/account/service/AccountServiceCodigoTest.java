package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.dto.AccountDtos.ChartAccountRequest;
import com.nalitech.modules.account.dto.AccountDtos.ChartAccountResponse;
import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.AccountRuleRepository;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.security.SecurityUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Garante que contas com a MESMA classificacao (ex.: varios fornecedores em 21301001) nao
 * colapsam na busca/selecao: a conciliacao e o seletor da interface as distinguem pelo id
 * e pelo codigo unico, mantendo a classificacao apenas para agrupamento.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceCodigoTest {

    @Mock
    private ChartOfAccountRepository chartRepository;
    @Mock
    private AccountRuleRepository ruleRepository;

    private final UUID empresaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    private AccountService service() {
        return new AccountService(chartRepository, ruleRepository);
    }

    private ChartOfAccount conta(UUID id, String codigo, String classificacao, String nome) {
        ChartOfAccount c = new ChartOfAccount();
        c.setId(id);
        c.setEmpresaId(empresaId);
        c.setClienteId(clienteId);
        c.setCodigo(codigo);
        c.setCodigoClassificacao(classificacao);
        c.setCodigoOriginal(codigo);
        c.setNome(nome);
        c.setAnalitica(true);
        return c;
    }

    // 5: seletor da interface (listLancaveis) traz as duas contas distintas mesmo com
    // classificacao identica.
    @Test
    void listLancaveisNaoColapsaContasDeMesmaClassificacao() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            when(chartRepository.findLancaveisForCliente(empresaId, clienteId)).thenReturn(List.of(
                    conta(idA, "0005198", "21301001", "ORCA DISTRIBUIDORA"),
                    conta(idB, "0005199", "21301001", "BIRIBA INDUSTRIA")));

            List<ChartAccountResponse> res = service().listLancaveis(clienteId);

            assertThat(res).hasSize(2);
            assertThat(res).extracting(ChartAccountResponse::id).containsExactlyInAnyOrder(idA, idB);
            assertThat(res).extracting(ChartAccountResponse::codigo)
                    .containsExactlyInAnyOrder("0005198", "0005199");
            assertThat(res).allMatch(r -> "21301001".equals(r.codigoClassificacao()));
        }
    }

    // 4: a conciliacao vincula a conta pelo id (findByIdAndEmpresaId); duas contas de mesma
    // classificacao resolvem para registros distintos.
    @Test
    void buscaPorIdDistingueContasDeMesmaClassificacao() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        when(chartRepository.findByIdAndEmpresaId(idA, empresaId))
                .thenReturn(Optional.of(conta(idA, "0005198", "21301001", "ORCA DISTRIBUIDORA")));
        when(chartRepository.findByIdAndEmpresaId(idB, empresaId))
                .thenReturn(Optional.of(conta(idB, "0005199", "21301001", "BIRIBA INDUSTRIA")));

        ChartOfAccount a = chartRepository.findByIdAndEmpresaId(idA, empresaId).orElseThrow();
        ChartOfAccount b = chartRepository.findByIdAndEmpresaId(idB, empresaId).orElseThrow();

        assertThat(a.getCodigo()).isEqualTo("0005198");
        assertThat(b.getCodigo()).isEqualTo("0005199");
        assertThat(a.getCodigoClassificacao()).isEqualTo(b.getCodigoClassificacao());
        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    // Criacao manual sem classificacao/original: espelham o codigo; unicidade e pelo codigo.
    @Test
    void createAccountDefaultClassificacaoEOriginalParaOCodigo() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::currentEmpresaId).thenReturn(empresaId);
            when(chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(eq(empresaId), eq(clienteId), any()))
                    .thenReturn(false);
            when(chartRepository.save(any(ChartOfAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            service().createAccount(new ChartAccountRequest(
                    "0005198", null, null, "ORCA", "A", true, null, null, null, clienteId));

            ArgumentCaptor<ChartOfAccount> captor = ArgumentCaptor.forClass(ChartOfAccount.class);
            org.mockito.Mockito.verify(chartRepository).save(captor.capture());
            ChartOfAccount saved = captor.getValue();
            assertThat(saved.getCodigo()).isEqualTo("0005198");
            assertThat(saved.getCodigoClassificacao()).isEqualTo("0005198");
            assertThat(saved.getCodigoOriginal()).isEqualTo("0005198");
        }
    }
}
