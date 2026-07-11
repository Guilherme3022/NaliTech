package com.nalitech.modules.parser.impl;

import com.nalitech.modules.parser.DocumentParser;
import com.nalitech.modules.parser.model.ParseResult;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "pdf".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] ocrTextContent) {
        String text = new String(ocrTextContent, StandardCharsets.UTF_8);
        return ParseResult.of(LineMovementExtractor.extract(text));
    }
}
