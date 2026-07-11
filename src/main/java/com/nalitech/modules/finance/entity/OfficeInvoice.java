package com.nalitech.modules.finance.entity;

import com.nalitech.shared.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "office_invoices")
@Getter
@Setter
@NoArgsConstructor
public class OfficeInvoice extends TenantEntity {

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "fee_id")
    private UUID feeId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDENTE;

    @Column(length = 30)
    private String provider;

    @Column(name = "external_id", length = 120)
    private String externalId;

    @Column(name = "boleto_url", length = 400)
    private String boletoUrl;

    @Column(name = "pix_copia_cola", length = 500)
    private String pixCopiaCola;

    @Column(name = "pix_qrcode", columnDefinition = "text")
    private String pixQrcode;

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;
}
