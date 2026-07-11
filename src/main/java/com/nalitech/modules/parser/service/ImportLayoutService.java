package com.nalitech.modules.parser.service;

import com.nalitech.modules.parser.dto.ImportLayoutDtos.ImportLayoutRequest;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.ImportLayoutResponse;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewRequest;
import com.nalitech.modules.parser.dto.ImportLayoutDtos.PreviewResponse;
import com.nalitech.modules.parser.entity.ImportLayout;
import com.nalitech.modules.parser.model.RawMovement;
import com.nalitech.modules.parser.repository.ImportLayoutRepository;
import com.nalitech.security.SecurityUtils;
import com.nalitech.shared.exception.BusinessException;
import com.nalitech.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImportLayoutService {

    private static final int MAX_PREVIEW_ROWS = 50;

    private final ImportLayoutRepository repository;

    public ImportLayoutService(ImportLayoutRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ImportLayoutResponse> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public ImportLayoutResponse create(ImportLayoutRequest request) {
        ImportLayout layout = new ImportLayout();
        layout.setEmpresaId(SecurityUtils.currentEmpresaId());
        apply(layout, request);
        return toResponse(repository.save(layout));
    }

    public ImportLayoutResponse update(UUID id, ImportLayoutRequest request) {
        ImportLayout layout = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Layout nao encontrado."));
        apply(layout, request);
        return toResponse(repository.save(layout));
    }

    public void delete(UUID id) {
        ImportLayout layout = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Layout nao encontrado."));
        repository.delete(layout);
    }

    /** Aplica o mapeamento a um CSV colado e devolve as primeiras linhas mapeadas. */
    @Transactional(readOnly = true)
    public PreviewResponse preview(PreviewRequest request) {
        List<String> linhas = request.conteudo().lines()
                .filter(l -> !l.isBlank())
                .toList();
        if (linhas.isEmpty()) {
            throw new BusinessException("Conteudo vazio.", HttpStatus.BAD_REQUEST);
        }
        char delimitador = linhas.get(0).contains(";") ? ';' : ',';
        List<String> headers = split(linhas.get(0), delimitador).stream()
                .map(h -> h.trim().toLowerCase())
                .toList();

        int idxData = indexOf(headers, request.colData());
        int idxValor = indexOf(headers, request.colValor());
        int idxDescricao = indexOf(headers, request.colDescricao());
        int idxDocumento = indexOf(headers, request.colDocumento());

        List<RawMovement> resultado = new ArrayList<>();
        for (int i = 1; i < linhas.size() && resultado.size() < MAX_PREVIEW_ROWS; i++) {
            List<String> cols = split(linhas.get(i), delimitador);
            resultado.add(new RawMovement(
                    at(cols, idxData), at(cols, idxValor), at(cols, idxDescricao), at(cols, idxDocumento)));
        }
        return new PreviewResponse(linhas.size() - 1, resultado);
    }

    private int indexOf(List<String> headers, String coluna) {
        if (coluna == null || coluna.isBlank()) {
            return -1;
        }
        return headers.indexOf(coluna.trim().toLowerCase());
    }

    private String at(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) {
            return null;
        }
        String value = cols.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> split(String line, char delimitador) {
        return Arrays.asList(line.split(String.valueOf(delimitador), -1));
    }

    private void apply(ImportLayout layout, ImportLayoutRequest request) {
        layout.setNome(request.nome());
        layout.setColData(request.colData());
        layout.setColValor(request.colValor());
        layout.setColDescricao(request.colDescricao());
        layout.setColDocumento(request.colDocumento());
        layout.setAtivo(request.ativo());
        layout.setClienteId(request.clienteId());
    }

    private ImportLayoutResponse toResponse(ImportLayout l) {
        return new ImportLayoutResponse(l.getId(), l.getNome(), l.getColData(), l.getColValor(),
                l.getColDescricao(), l.getColDocumento(), l.isAtivo(), l.getClienteId());
    }
}
