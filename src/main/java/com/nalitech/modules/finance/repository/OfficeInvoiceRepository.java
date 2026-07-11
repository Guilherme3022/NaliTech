package com.nalitech.modules.finance.repository;

import com.nalitech.modules.finance.entity.InvoiceStatus;
import com.nalitech.modules.finance.entity.OfficeInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeInvoiceRepository extends JpaRepository<OfficeInvoice, UUID> {

    Page<OfficeInvoice> findByEmpresaId(UUID empresaId, Pageable pageable);

    Optional<OfficeInvoice> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<OfficeInvoice> findByProviderAndExternalId(String provider, String externalId);

    List<OfficeInvoice> findByEmpresaIdAndStatusAndVencimentoBefore(
            UUID empresaId, InvoiceStatus status, LocalDate data);
}
