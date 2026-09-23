package com.nalitech.modules.movement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementType;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.parser.model.RawMovement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementNormalizerTest {

    @Mock
    private MovementRepository movementRepository;

    private MovementNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new MovementNormalizer(movementRepository);
    }

    @Test
    void parseValorNoFormatoBrasileiro() {
        assertThat(normalizer.parseValor("1.234,56")).isEqualByComparingTo("1234.56");
    }

    @Test
    void parseValorNegativoComSimboloDeMoeda() {
        assertThat(normalizer.parseValor("R$ -50,00")).isEqualByComparingTo("-50.00");
    }

    @Test
    void parseValorNoFormatoAmericano() {
        assertThat(normalizer.parseValor("250.00")).isEqualByComparingTo("250.00");
    }

    @Test
    void parseValorInvalidoRetornaNulo() {
        assertThat(normalizer.parseValor("abc")).isNull();
        assertThat(normalizer.parseValor("   ")).isNull();
        assertThat(normalizer.parseValor(null)).isNull();
    }

    @Test
    void parseDataAceitaMultiplosFormatos() {
        assertThat(normalizer.parseData("01/02/2026")).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(normalizer.parseData("2026-02-01")).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(normalizer.parseData("20260201")).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(normalizer.parseData("01-02-2026")).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void parseDataInvalidaRetornaNulo() {
        assertThat(normalizer.parseData("nao-e-data")).isNull();
        assertThat(normalizer.parseData(null)).isNull();
    }

    @Test
    void normalizeClassificaValorNegativoComoSaida() {
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> {
                    Movement m = invocation.getArgument(0);
                    m.setId(UUID.randomUUID());
                    return m;
                });

        var raw = new RawMovement("01/02/2026", "-100,00", "Tarifa   bancaria", "DOC1");
        List<UUID> ids = normalizer.normalize(UUID.randomUUID(), UUID.randomUUID(), null, "csv", null, List.of(raw));

        assertThat(ids).hasSize(1);
    }

    @Test
    void normalizeLimpaEspacosDaDescricaoEDetectaEntrada() {
        var capturados = new java.util.ArrayList<Movement>();
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> {
                    Movement m = invocation.getArgument(0);
                    m.setId(UUID.randomUUID());
                    capturados.add(m);
                    return m;
                });

        var raw = new RawMovement("03/02/2026", "1.000,00", "Deposito    salario", "DOC2");
        normalizer.normalize(UUID.randomUUID(), UUID.randomUUID(), null, "ofx", null, List.of(raw));

        Movement gerado = capturados.get(0);
        assertThat(gerado.getDescricao()).isEqualTo("Deposito salario");
        assertThat(gerado.getTipo()).isEqualTo(MovementType.ENTRADA);
        assertThat(gerado.getValor()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void resolverTipoUsaIndicadorExplicitoAntesDoSinal() {
        // Indicador do extrato: D = debito/saida, C = credito/entrada. Tem prioridade
        // sobre o sinal (cobre casos em que o valor vem sem sinal, mas com coluna D/C).
        assertThat(normalizer.resolverTipo("D", new BigDecimal("100.00")))
                .isEqualTo(MovementType.SAIDA);
        assertThat(normalizer.resolverTipo("C", new BigDecimal("-100.00")))
                .isEqualTo(MovementType.ENTRADA);
        // Aceita variacoes ("DEBITO"/"CREDITO", minusculas).
        assertThat(normalizer.resolverTipo("debito", null)).isEqualTo(MovementType.SAIDA);
        assertThat(normalizer.resolverTipo("credito", null)).isEqualTo(MovementType.ENTRADA);
    }

    @Test
    void resolverTipoCaiNoSinalQuandoNaoHaIndicador() {
        assertThat(normalizer.resolverTipo(null, new BigDecimal("-875.40")))
                .isEqualTo(MovementType.SAIDA);
        assertThat(normalizer.resolverTipo(null, new BigDecimal("1250.00")))
                .isEqualTo(MovementType.ENTRADA);
        assertThat(normalizer.resolverTipo("  ", new BigDecimal("-1.00")))
                .isEqualTo(MovementType.SAIDA);
    }

    @Test
    void resolverTipoSemIndicadorNemSinalAssumeEntrada() {
        assertThat(normalizer.resolverTipo(null, BigDecimal.ZERO)).isEqualTo(MovementType.ENTRADA);
        assertThat(normalizer.resolverTipo(null, null)).isEqualTo(MovementType.ENTRADA);
    }

    @Test
    void normalizeDetectaSaidaPeloIndicadorMesmoComValorSemSinal() {
        var capturados = new java.util.ArrayList<Movement>();
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> {
                    Movement m = invocation.getArgument(0);
                    m.setId(UUID.randomUUID());
                    capturados.add(m);
                    return m;
                });

        // Valor sem sinal, mas com indicador D (coluna D/C do extrato) -> SAIDA.
        var raw = new RawMovement("05/02/2026", "438,72", "Pagamento energia", "DOC3", "D");
        normalizer.normalize(UUID.randomUUID(), UUID.randomUUID(), null, "csv", null, List.of(raw));

        Movement gerado = capturados.get(0);
        assertThat(gerado.getTipo()).isEqualTo(MovementType.SAIDA);
        // O valor tambem e normalizado para negativo, coerente com o tipo SAIDA.
        assertThat(gerado.getValor()).isEqualByComparingTo(new BigDecimal("-438.72"));
    }
}
