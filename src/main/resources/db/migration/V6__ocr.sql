-- E5 - OCR

CREATE TABLE ocr_results (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id        UUID NOT NULL,
    upload_id         UUID NOT NULL,
    texto_extraido    TEXT,
    tabelas_extraidas JSONB,
    confianca         NUMERIC(5,2),
    motor_usado       VARCHAR(40),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(150)
);

CREATE INDEX idx_ocr_results_upload ON ocr_results (upload_id);
