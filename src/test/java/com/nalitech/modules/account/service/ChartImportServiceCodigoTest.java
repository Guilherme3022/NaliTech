package com.nalitech.modules.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.account.service.ChartImportService.ContaSelecionada;
import com.nalitech.modules.account.service.ChartImportService.ImportResult;
import com.nalitech.modules.account.service.ChartImportService.PreviewConta;
import com.nalitech.modules.client.entity.Client;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.security.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChartImportServiceCodigoTest {

    @Mock
    private ChartOfAccountRepository chartRepository;
    @Mock
    private ClientRepository clientRepository;

    // Construido dentro de cada teste (apos a injecao dos mocks). Usa um parser REAL.
    private ChartImportService service() {
        return new ChartImportService(chartRepository, clientRepository, new ChartLayoutParser());
    }

    private final UUID empresaId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    private static final String ARQUIVO = """
            000016521301               FORNECEDORES                            S
            000519821301001            ORCA DISTRIBUIDORA E TRANSPORTES LTDA   A
            000519921301001            BIRIBA INDUSTRIA DE BEBIDAS LTDA        A
            000520021301001            ITALIANY ALIMENTOS LTDA                 A
            000520121301001            VALE FERTIL INDUSTRIAS ALIMENTICIAS LTDA A
            000520221301001            DOCE MINEIRO LTDA                       A
            """;

    private byte[] bytes() {
        return ARQUIVO.getBytes(StandardCharsets.UTF_8);
    }

    private void stubCliente() {
        when(clientRepository.findByIdAndEmpresaId(clienteId, empresaId))
                .thenReturn(Optional.of(new Client()));
    }

    // 2: nenhuma colisao entre os fornecedores (mesma classificacao, reduzidos distintos).
    @Test
    void previewNaoColideEntreFornecedores() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::requireEmpresaId).thenReturn(empresaId);
            stubCliente();
            // Nenhuma conta existe ainda.
            when(chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(eq(empresaId), eq(clienteId), any()))
                    .thenReturn(false);

            List<PreviewConta> previa = service().preview(clienteId, "plano.txt", bytes());

            List<PreviewConta> fornecedores = previa.stream()
                    .filter(p -> "21301001".equals(p.codigoClassificacao()))
                    .toList();
            assertThat(fornecedores).hasSize(5);
            // Todos importaveis (nao colidem entre si).
            assertThat(fornecedores).allMatch(PreviewConta::importavel);
            // Identificador unico distinto para cada um; classificacao compartilhada.
            assertThat(fornecedores.stream().map(PreviewConta::codigo).distinct().toList())
                    .containsExactlyInAnyOrder("0005198", "0005199", "0005200", "0005201", "0005202");
        }
    }

    // 3 + 8: confirmImport persiste reduzido/classificacao/original e o tipo A (nao na descricao).
    @Test
    void confirmImportPersisteOsTresCodigos() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::requireEmpresaId).thenReturn(empresaId);
            stubCliente();
            when(chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(eq(empresaId), eq(clienteId), any()))
                    .thenReturn(false);

            List<ContaSelecionada> sel = List.of(new ContaSelecionada(
                    "0005198", "21301001", "000519821301001",
                    "ORCA DISTRIBUIDORA E TRANSPORTES LTDA", "A", true, null));

            ImportResult r = service().confirmImport(clienteId, sel);

            assertThat(r.contasCriadas()).isEqualTo(1);
            ArgumentCaptor<ChartOfAccount> captor = ArgumentCaptor.forClass(ChartOfAccount.class);
            verify(chartRepository).save(captor.capture());
            ChartOfAccount saved = captor.getValue();
            assertThat(saved.getCodigo()).isEqualTo("0005198");
            assertThat(saved.getCodigoClassificacao()).isEqualTo("21301001");
            assertThat(saved.getCodigoOriginal()).isEqualTo("000519821301001");
            assertThat(saved.getNome()).isEqualTo("ORCA DISTRIBUIDORA E TRANSPORTES LTDA");
            assertThat(saved.getAnalitica()).isTrue();
            assertThat(saved.getTipo()).isEqualTo("ANALITICA");
        }
    }

    // 6: reimportacao do mesmo arquivo nao duplica (contas ja existentes sao ignoradas).
    @Test
    void reimportacaoNaoDuplica() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::requireEmpresaId).thenReturn(empresaId);
            stubCliente();
            // Simula que TODAS ja existem (reimport).
            when(chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(eq(empresaId), eq(clienteId), any()))
                    .thenReturn(true);

            List<ContaSelecionada> sel = new ArrayList<>();
            sel.add(new ContaSelecionada("0005198", "21301001", "000519821301001",
                    "ORCA", "A", true, null));
            sel.add(new ContaSelecionada("0005199", "21301001", "000519921301001",
                    "BIRIBA", "A", true, null));

            ImportResult r = service().confirmImport(clienteId, sel);

            assertThat(r.contasCriadas()).isZero();
            assertThat(r.contasIgnoradas()).isEqualTo(2);
            verify(chartRepository, times(0)).save(any());
        }
    }

    // 7: compatibilidade com plano de codigo unico (delimitado) -> tres campos coerentes.
    @Test
    void compativelComCodigoUnicoDelimitado() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::requireEmpresaId).thenReturn(empresaId);
            stubCliente();
            when(chartRepository.existsByEmpresaIdAndClienteIdAndCodigo(eq(empresaId), eq(clienteId), any()))
                    .thenReturn(false);

            String csv = "codigo,nome,tipo\n1.1.01.001,CAIXA GERAL,A\n";
            List<PreviewConta> previa =
                    service().preview(clienteId, "plano.csv", csv.getBytes(StandardCharsets.UTF_8));

            PreviewConta c = previa.get(0);
            assertThat(c.codigo()).isEqualTo("1.1.01.001");
            assertThat(c.codigoClassificacao()).isEqualTo("1.1.01.001");
            assertThat(c.codigoOriginal()).isEqualTo("1.1.01.001");
            assertThat(c.importavel()).isTrue();
        }
    }
}
