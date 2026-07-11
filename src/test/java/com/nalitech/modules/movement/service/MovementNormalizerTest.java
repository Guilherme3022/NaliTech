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
        List<UUID> ids = normalizer.normalize(UUID.randomUUID(), UUID.randomUUID(), "csv", List.of(raw));

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
        normalizer.normalize(UUID.randomUUID(), UUID.randomUUID(), "ofx", List.of(raw));

        Movement gerado = capturados.get(0);
        assertThat(gerado.getDescricao()).isEqualTo("Deposito salario");
        assertThat(gerado.getTipo()).isEqualTo(MovementType.ENTRADA);
        assertThat(gerado.getValor()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
