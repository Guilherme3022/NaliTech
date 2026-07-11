package com.nalitech.modules.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalitech.modules.parser.impl.CsvParser;
import com.nalitech.modules.parser.impl.OfxParser;
import com.nalitech.modules.parser.model.ParseResult;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ParsersTest {

    @Test
    void csvParserLeColunasPorCabecalho() {
        String csv = "data;valor;historico;documento\n"
                + "01/02/2026;1.234,56;Pagamento fornecedor;DOC1\n"
                + "03/02/2026;-50,00;Tarifa bancaria;DOC2\n";
        ParseResult result = new CsvParser().parse(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result.movements()).hasSize(2);
        assertThat(result.movements().get(0).descricao()).isEqualTo("Pagamento fornecedor");
        assertThat(result.movements().get(1).valor()).isEqualTo("-50,00");
    }

    @Test
    void ofxParserExtraiTransacoes() {
        String ofx = """
                <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
                <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260201<TRNAMT>-100.00<FITID>1<MEMO>Compra</STMTTRN>
                <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260203<TRNAMT>250.00<FITID>2<MEMO>Deposito</STMTTRN>
                </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
                """;
        ParseResult result = new OfxParser().parse(ofx.getBytes(StandardCharsets.UTF_8));

        assertThat(result.movements()).hasSize(2);
        assertThat(result.movements().get(0).descricao()).isEqualTo("Compra");
        assertThat(result.movements().get(1).valor()).isEqualTo("250.00");
    }
}
