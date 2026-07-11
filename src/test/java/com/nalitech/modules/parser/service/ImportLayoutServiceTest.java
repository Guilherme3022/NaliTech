package com.nalitech.modules.parser.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewRequest;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewResponse;
import org.junit.jupiter.api.Test;

class ImportLayoutServiceTest {

    // preview() nao usa o repositorio, entao pode ser nulo aqui.
    private final ImportLayoutService service = new ImportLayoutService(null);

    @Test
    void mapeiaColunasPorNomeComDelimitadorPontoEVirgula() {
        String csv = """
                Data;Valor;Historico;Doc
                01/02/2026;100,00;TARIFA BANCARIA;123
                02/02/2026;-50,00;PIX ENVIADO;456
                """;
        PreviewResponse r = service.preview(
                new PreviewRequest(csv, "Data", "Valor", "Historico", "Doc"));

        assertThat(r.total()).isEqualTo(2);
        assertThat(r.linhas()).hasSize(2);
        assertThat(r.linhas().get(0).data()).isEqualTo("01/02/2026");
        assertThat(r.linhas().get(0).valor()).isEqualTo("100,00");
        assertThat(r.linhas().get(0).descricao()).isEqualTo("TARIFA BANCARIA");
        assertThat(r.linhas().get(0).documento()).isEqualTo("123");
    }

    @Test
    void ignoraMaiusculasNoNomeDaColunaEUsaVirgulaComoDelimitador() {
        String csv = "data,valor,descricao\n10/03/2026,200.00,COMPRA";
        PreviewResponse r = service.preview(
                new PreviewRequest(csv, "DATA", "Valor", "DESCRICAO", null));

        assertThat(r.linhas()).hasSize(1);
        assertThat(r.linhas().get(0).data()).isEqualTo("10/03/2026");
        assertThat(r.linhas().get(0).valor()).isEqualTo("200.00");
        assertThat(r.linhas().get(0).descricao()).isEqualTo("COMPRA");
        assertThat(r.linhas().get(0).documento()).isNull();
    }
}
