package com.nalitech.modules.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.account.repository.ChartOfAccountRepository;
import com.nalitech.modules.client.entity.Client;
import com.nalitech.modules.client.repository.ClientRepository;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementType;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService.ExportFile;
import com.nalitech.modules.reconciliation.service.ConciliacaoExportService.ExportOptions;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConciliacaoExportServiceTest {

    @Mock private ConciliacaoService conciliacaoService;
    @Mock private MovementRepository movementRepository;
    @Mock private ChartOfAccountRepository chartRepository;
    @Mock private ClientRepository clientRepository;

    private ConciliacaoExportService service;

    private final UUID empresa = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConciliacaoExportService(conciliacaoService, movementRepository,
                chartRepository, clientRepository);
    }

    private ChartOfAccount conta(UUID id, String codigo) {
        ChartOfAccount c = new ChartOfAccount();
        c.setId(id);
        c.setCodigo(codigo);
        return c;
    }

    @Test
    void geraLinhaDePartidaDobradaNoFormatoConfirmado() {
        UUID debito = UUID.randomUUID();
        UUID credito = UUID.randomUUID();

        Movement m = new Movement();
        m.setId(UUID.randomUUID());
        m.setEmpresaId(empresa);
        m.setClienteId(cliente);
        m.setData(LocalDate.of(2026, 7, 10));
        m.setValor(new BigDecimal("-477.20"));
        m.setTipo(MovementType.SAIDA);
        m.setDescricao("Vlr ref pagto bcontrol 1191 parc 1");
        m.setContaDebitoId(debito);
        m.setContaCreditoId(credito);

        when(movementRepository.findByEmpresaIdAndClienteIdAndDataBetweenOrderByData(
                any(), any(), any(), any())).thenReturn(List.of(m));
        when(chartRepository.findAllById(any()))
                .thenReturn(List.of(conta(debito, "4921"), conta(credito, "1362")));
        Client c = new Client();
        c.setCodigoEmpresaContabil(47);
        when(clientRepository.findByIdAndEmpresaId(cliente, empresa)).thenReturn(Optional.of(c));

        ExportFile file = service.exportCompetencia(empresa, cliente, LocalDate.of(2026, 7, 1),
                ExportOptions.padrao());

        String txt = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(txt)
                .isEqualTo("10/07/2026;4921;;1362;;477,20;VLR REF PAGTO BCONTROL 1191 PARC 1;47\n");
        assertThat(file.filename()).endsWith(".txt");
    }

    @Test
    void contaFaltandoFicaVaziaEValorSempreVirgula() {
        Movement m = new Movement();
        m.setId(UUID.randomUUID());
        m.setEmpresaId(empresa);
        m.setClienteId(cliente);
        m.setData(LocalDate.of(2026, 7, 5));
        m.setValor(new BigDecimal("1250.00"));
        m.setTipo(MovementType.ENTRADA);
        m.setDescricao("PIX RECEBIDO CLIENTE ALFA");
        // sem contas escolhidas -> campos de conta vazios

        when(movementRepository.findByEmpresaIdAndClienteIdAndDataBetweenOrderByData(
                any(), any(), any(), any())).thenReturn(List.of(m));
        when(clientRepository.findByIdAndEmpresaId(cliente, empresa)).thenReturn(Optional.empty());

        ExportFile file = service.exportCompetencia(empresa, cliente, LocalDate.of(2026, 7, 1),
                ExportOptions.padrao());

        String txt = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(txt).isEqualTo("05/07/2026;;;;;1250,00;PIX RECEBIDO CLIENTE ALFA;\n");
    }
}
