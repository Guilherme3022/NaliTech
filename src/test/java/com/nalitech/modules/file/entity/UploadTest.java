package com.nalitech.modules.file.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadTest {

    @Test
    void novoUploadComecaComoRecebido() {
        assertThat(new Upload().getStatus()).isEqualTo(UploadStatus.RECEBIDO);
    }

    @Test
    void avancarAtualizaStatusEEtapa() {
        Upload upload = new Upload();

        upload.avancar(UploadStatus.PROCESSANDO, "PARSER");

        assertThat(upload.getStatus()).isEqualTo(UploadStatus.PROCESSANDO);
        assertThat(upload.getEtapaAtual()).isEqualTo("PARSER");
        assertThat(upload.getErroMensagem()).isNull();
    }

    @Test
    void marcarErroRegistraStatusEtapaEMensagem() {
        Upload upload = new Upload();
        upload.avancar(UploadStatus.VALIDANDO, "OCR");

        upload.marcarErro("OCR", "arquivo ilegivel");

        assertThat(upload.getStatus()).isEqualTo(UploadStatus.ERRO);
        assertThat(upload.getEtapaAtual()).isEqualTo("OCR");
        assertThat(upload.getErroMensagem()).isEqualTo("arquivo ilegivel");
    }
}
