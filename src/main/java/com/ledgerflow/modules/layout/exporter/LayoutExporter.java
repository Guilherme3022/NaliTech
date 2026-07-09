package com.ledgerflow.modules.layout.exporter;

import com.ledgerflow.modules.movement.entity.Movement;
import java.util.List;

public interface LayoutExporter {

    boolean supports(String sistema);

    String sistema();

    ExportedFile export(List<Movement> movements);
}
