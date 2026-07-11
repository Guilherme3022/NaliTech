package com.nalitech.modules.movement.service;

import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.modules.movement.entity.MovementStatus;
import com.nalitech.modules.movement.entity.MovementType;
import com.nalitech.modules.movement.repository.MovementRepository;
import com.nalitech.modules.parser.model.RawMovement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class MovementNormalizer {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("dd/MM/yy"));

    private final MovementRepository movementRepository;

    public MovementNormalizer(MovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    public List<UUID> normalize(UUID uploadId, UUID empresaId, UUID clienteId, String origem,
                                List<RawMovement> rawMovements) {
        return rawMovements.stream()
                .map(raw -> toMovement(uploadId, empresaId, clienteId, origem, raw))
                .map(movementRepository::save)
                .map(Movement::getId)
                .toList();
    }

    private Movement toMovement(UUID uploadId, UUID empresaId, UUID clienteId, String origem,
                                RawMovement raw) {
        BigDecimal valor = parseValor(raw.valor());
        Movement movement = new Movement();
        movement.setEmpresaId(empresaId);
        movement.setClienteId(clienteId);
        movement.setUploadId(uploadId);
        movement.setOrigem(origem);
        movement.setData(parseData(raw.data()));
        movement.setValor(valor);
        movement.setTipo(valor != null && valor.signum() < 0 ? MovementType.SAIDA : MovementType.ENTRADA);
        movement.setDescricao(limparDescricao(raw.descricao()));
        movement.setDocumento(raw.documento());
        movement.setStatus(MovementStatus.NORMALIZADO);
        return movement;
    }

    BigDecimal parseValor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^0-9,.-]", "").trim();
        if (cleaned.contains(",") && cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {

            cleaned = cleaned.replace(".", "").replace(",", ".");
        } else {
            cleaned = cleaned.replace(",", "");
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            log.debug("Valor nao parseavel: {}", raw);
            return null;
        }
    }

    LocalDate parseData(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (Exception ignored) {

            }
        }
        log.debug("Data nao parseavel: {}", raw);
        return null;
    }

    private String limparDescricao(String descricao) {
        if (descricao == null) {
            return null;
        }

        return descricao.replaceAll("\\s+", " ").trim();
    }
}
