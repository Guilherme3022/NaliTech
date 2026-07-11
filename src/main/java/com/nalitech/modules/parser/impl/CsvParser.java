package com.nalitech.modules.parser.impl;

import com.nalitech.modules.parser.DocumentParser;
import com.nalitech.modules.parser.model.ParseResult;
import com.nalitech.modules.parser.model.RawMovement;
import com.nalitech.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CsvParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "csv".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] content) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setDelimiter(detectDelimiter(content))
                .build();

        List<RawMovement> movements = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                movements.add(new RawMovement(
                        get(record, ColumnResolver.DATA),
                        get(record, ColumnResolver.VALOR),
                        get(record, ColumnResolver.DESCRICAO),
                        get(record, ColumnResolver.DOCUMENTO)));
            }
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler CSV: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ParseResult.of(movements);
    }

    private String get(CSVRecord record, List<String> candidates) {
        for (String candidate : candidates) {
            for (String header : record.getParser().getHeaderNames()) {
                if (header != null && header.trim().equalsIgnoreCase(candidate)) {
                    return record.get(header);
                }
            }
        }
        return null;
    }

    private char detectDelimiter(byte[] content) {
        String head = new String(content, 0, Math.min(content.length, 512), StandardCharsets.UTF_8);
        long semicolons = head.chars().filter(c -> c == ';').count();
        long commas = head.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }
}
