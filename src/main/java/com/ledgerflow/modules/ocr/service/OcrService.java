package com.ledgerflow.modules.ocr.service;

import com.ledgerflow.modules.ocr.entity.OcrResult;
import com.ledgerflow.modules.ocr.repository.OcrResultRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class OcrService {

    private final OcrResultRepository ocrResultRepository;
    private final String tessDataPath;
    private final String tessLanguage;

    public OcrService(OcrResultRepository ocrResultRepository,
                      @Value("${OCR_TESSDATA_PATH:/usr/share/tesseract-ocr/5/tessdata}") String tessDataPath,
                      @Value("${OCR_LANGUAGE:por}") String tessLanguage) {
        this.ocrResultRepository = ocrResultRepository;
        this.tessDataPath = tessDataPath;
        this.tessLanguage = tessLanguage;
    }

    public OcrResult process(UUID uploadId, UUID empresaId, String tipoMime, byte[] content) {
        String texto;
        String motor;
        BigDecimal confianca;

        if (isPdf(tipoMime)) {
            String nativo = extractPdfText(content);
            if (nativo != null && !nativo.isBlank()) {
                texto = nativo;
                motor = "PDFBOX";
                confianca = BigDecimal.valueOf(99);
            } else {
                texto = ocrPdf(content);
                motor = "TESSERACT_PDF";
                confianca = texto.isBlank() ? BigDecimal.ZERO : BigDecimal.valueOf(70);
            }
        } else if (isImage(tipoMime)) {
            texto = ocrImage(content);
            motor = "TESSERACT_IMG";
            confianca = texto.isBlank() ? BigDecimal.ZERO : BigDecimal.valueOf(70);
        } else {

            texto = "";
            motor = "NAO_APLICAVEL";
            confianca = BigDecimal.ZERO;
        }

        return persist(uploadId, empresaId, texto, motor, confianca);
    }

    private OcrResult persist(UUID uploadId, UUID empresaId, String texto, String motor,
                              BigDecimal confianca) {
        OcrResult result = ocrResultRepository.findByUploadId(uploadId).orElseGet(OcrResult::new);
        result.setEmpresaId(empresaId);
        result.setUploadId(uploadId);
        result.setTextoExtraido(texto);
        result.setMotorUsado(motor);
        result.setConfianca(confianca);
        return ocrResultRepository.save(result);
    }

    private String extractPdfText(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            log.warn("Falha ao extrair texto nativo do PDF: {}", ex.getMessage());
            return null;
        }
    }

    private String ocrPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            Tesseract tesseract = newTesseract();
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder sb = new StringBuilder();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300);
                sb.append(tesseract.doOCR(image)).append('\n');
            }
            return sb.toString();
        } catch (Throwable ex) {
            log.warn("OCR de PDF indisponivel: {}", ex.getMessage());
            return "";
        }
    }

    private String ocrImage(byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            return newTesseract().doOCR(image);
        } catch (Throwable ex) {
            log.warn("OCR de imagem indisponivel: {}", ex.getMessage());
            return "";
        }
    }

    private Tesseract newTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(tessLanguage);
        return tesseract;
    }

    private boolean isPdf(String mime) {
        return mime != null && mime.contains("pdf");
    }

    private boolean isImage(String mime) {
        return mime != null && mime.startsWith("image");
    }
}
