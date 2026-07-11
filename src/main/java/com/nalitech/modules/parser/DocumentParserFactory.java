package com.nalitech.modules.parser;

import com.nalitech.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public DocumentParser resolve(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase();
        return parsers.stream()
                .filter(parser -> parser.supports(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Nenhum parser disponivel para o formato: " + extension,
                        HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
