package com.nalitech.modules.parser.impl;

import com.nalitech.modules.parser.DocumentParser;
import com.nalitech.modules.parser.model.ParseResult;
import com.nalitech.modules.parser.model.RawMovement;
import com.nalitech.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExcelParser implements DocumentParser {

    private final DataFormatter formatter = new DataFormatter();

    @Override
    public boolean supports(String extension) {
        return "xlsx".equals(extension) || "xls".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] content) {
        List<RawMovement> movements = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                return ParseResult.of(movements);
            }
            List<String> headers = readRow(header);
            int idxData = ColumnResolver.indexOf(headers, ColumnResolver.DATA);
            int idxValor = ColumnResolver.indexOf(headers, ColumnResolver.VALOR);
            int idxDescricao = ColumnResolver.indexOf(headers, ColumnResolver.DESCRICAO);
            int idxDocumento = ColumnResolver.indexOf(headers, ColumnResolver.DOCUMENTO);

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                List<String> cells = readRow(row);
                movements.add(new RawMovement(
                        ColumnResolver.at(cells, idxData),
                        ColumnResolver.at(cells, idxValor),
                        ColumnResolver.at(cells, idxDescricao),
                        ColumnResolver.at(cells, idxDocumento)));
            }
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler planilha: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ParseResult.of(movements);
    }

    private List<String> readRow(Row row) {
        List<String> values = new ArrayList<>();
        int lastCell = Math.max(row.getLastCellNum(), 0);
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            values.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        return values;
    }
}
