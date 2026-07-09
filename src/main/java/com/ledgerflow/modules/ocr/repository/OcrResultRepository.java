package com.ledgerflow.modules.ocr.repository;

import com.ledgerflow.modules.ocr.entity.OcrResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrResultRepository extends JpaRepository<OcrResult, UUID> {

    Optional<OcrResult> findByUploadId(UUID uploadId);
}
