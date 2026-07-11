package com.nalitech.modules.ocr.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ocr_results")
@Getter
@Setter
@NoArgsConstructor
public class OcrResult extends TenantEntity {

    @Column(name = "upload_id", nullable = false)
    private UUID uploadId;

    @Column(name = "texto_extraido", columnDefinition = "text")
    private String textoExtraido;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tabelas_extraidas", columnDefinition = "jsonb")
    private String tabelasExtraidas;

    @Column(precision = 5, scale = 2)
    private BigDecimal confianca;

    @Column(name = "motor_usado", length = 40)
    private String motorUsado;
}
