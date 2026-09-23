package com.nalitech.modules.parser.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.parser.model.RawMovement;
import java.util.List;
import org.junit.jupiter.api.Test;

class LineMovementExtractorTest {

    @Test
    void generico_banco_do_brasil_usa_marcador_credito_debito() {
        String texto = String.join("\n",
                "29/05/2026 0000 00000 000 Saldo Anterior 48.047,11 C",
                "01/06/2026 0000 14134 612 Recebimento Fornecedor 507 77,12 C",
                "02/06/2026 0000 13105 109 Pagamento de Boleto 60.201 26.503,93 D 62.013,65 C");

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        // A linha de saldo e ignorada; sobram 2 movimentos.
        assertThat(movs).hasSize(2);
        assertThat(movs.get(0).valor()).isEqualTo("77,12"); // credito (positivo)
        // Debito vira negativo e usa o valor da transacao, nao o saldo.
        assertThat(movs.get(1).valor()).isEqualTo("-26.503,93");
    }

    @Test
    void generico_contas_a_pagar_usa_data_de_pagamento_e_valor_negativo() {
        String texto = "R$ 360,0002/07/2026 24/06/2026 R$0,00 R$0,00R$0,00NF-e: 001 DO BRASIL LTDA1/1";

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).valor()).isEqualTo("-360,00"); // saida
        assertThat(movs.get(0).data()).isEqualTo("24/06/2026"); // data de pagamento (2a data)
    }

    @Test
    void generico_bb_anexa_contraparte_e_cnpj_da_linha_seguinte() {
        String texto = String.join("\n",
                "01/06/2026 0000 14134 612 Recebimento Fornecedor 507 77,12 C",
                "92.559.830/0001-71 GREEN CARD SA REFEI");

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).descricao()).contains("GREEN CARD");
        assertThat(movs.get(0).documento()).isEqualTo("92559830000171");
    }

    @Test
    void banrisul_usa_dia_isolado_mes_do_cabecalho_e_sinal_de_menos() {
        String texto = String.join("\n",
                "------------------------- MOVIMENTOS DA CONTA CORRENTE -------------------------",
                "++  MOVIMENTOS JUN/2026",
                "SALDO ANT EM 29/05/2026                                                4,87",
                "01  CREDITO TITULOS                                     000005           997,14",
                "PIX RECEBIDO                                        000000            12,15",
                "NOME: MARISELE ROCHA",
                "PAGAMENTO GUIA DE ARRECADACAO                       506181          4.740,00-");

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(3); // saldo ignorado
        assertThat(movs.get(0).data()).isEqualTo("01/06/2026");
        assertThat(movs.get(0).valor()).isEqualTo("997,14");
        // Contraparte da linha NOME e anexada a descricao do PIX.
        assertThat(movs.get(1).valor()).isEqualTo("12,15");
        assertThat(movs.get(1).descricao()).contains("MARISELE ROCHA");
        // Debito (sinal de menos no fim) vira negativo.
        assertThat(movs.get(2).valor()).isEqualTo("-4.740,00");
        // Banrisul tambem marca a natureza: debito = D, credito = C.
        assertThat(movs.get(0).tipoIndicador()).isEqualTo("C");
        assertThat(movs.get(2).tipoIndicador()).isEqualTo("D");
    }

    @Test
    void generico_saida_com_sinal_de_menos_a_frente_do_valor() {
        // Caso mais comum de extrato: a saida vem com "-" a frente do valor. Antes o padrao
        // de valor descartava o sinal e tudo virava ENTRADA — este teste trava a regressao.
        String texto = String.join("\n",
                "01/09/2026 PIX RECEBIDO CLIENTE ALFA 1.250,00",
                "01/09/2026 PAGAMENTO FORNECEDOR XPTO -875,40");

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(2);
        // Entrada: valor positivo, sem indicador (deriva do sinal na normalizacao).
        assertThat(movs.get(0).valor()).isEqualTo("1.250,00");
        // Saida: sinal de menos a frente e preservado e a natureza vira D (debito/saida).
        assertThat(movs.get(1).valor()).isEqualTo("-875,40");
        assertThat(movs.get(1).tipoIndicador()).isEqualTo("D");
    }

    @Test
    void generico_saida_entre_parenteses_vira_negativo() {
        String texto = "04/09/2026 TARIFA BANCARIA (45,00)";

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).valor()).isEqualTo("-45,00");
        assertThat(movs.get(0).tipoIndicador()).isEqualTo("D");
    }

    @Test
    void generico_valor_positivo_sem_marcador_fica_sem_indicador() {
        // Sem "-", sem C/D e sem parenteses: nao da para saber a natureza pelo texto;
        // fica sem indicador (a normalizacao assume ENTRADA pelo sinal positivo).
        String texto = "02/09/2026 RECEBIMENTO BOLETO CLIENTE BETA 3.500,00";

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(1);
        assertThat(movs.get(0).valor()).isEqualTo("3.500,00");
        assertThat(movs.get(0).tipoIndicador()).isNull();
    }

    @Test
    void generico_marcador_dc_define_a_natureza() {
        String texto = String.join("\n",
                "03/09/2026 RECEBIMENTO CLIENTE GAMMA 1.800,00 C",
                "03/09/2026 PAGAMENTO NF 45872 FORNECEDOR XPTO 1.200,50 D");

        List<RawMovement> movs = LineMovementExtractor.extract(texto);

        assertThat(movs).hasSize(2);
        assertThat(movs.get(0).valor()).isEqualTo("1.800,00");
        assertThat(movs.get(0).tipoIndicador()).isEqualTo("C");
        assertThat(movs.get(1).valor()).isEqualTo("-1.200,50");
        assertThat(movs.get(1).tipoIndicador()).isEqualTo("D");
    }
}
