package com.ledgerflow.modules.parser.impl;

import com.ledgerflow.modules.parser.DocumentParser;
import com.ledgerflow.modules.parser.model.ParseResult;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class TxtParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "txt".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        return ParseResult.of(LineMovementExtractor.extract(text));
    }
}
