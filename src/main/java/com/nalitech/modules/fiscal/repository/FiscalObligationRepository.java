package com.nalitech.modules.fiscal.repository;

import com.nalitech.modules.fiscal.entity.FiscalObligation;
import com.nalitech.modules.fiscal.entity.ObligationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalObligationRepository extends JpaRepository<FiscalObligation, UUID> {

    Page<FiscalObligation> findByEmpresaId(UUID empresaId, Pageable pageable);

    Optional<FiscalObligation> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<FiscalObligation> findByEmpresaIdAndStatusAndVencimentoBetween(
            UUID empresaId, ObligationStatus status, LocalDate inicio, LocalDate fim);

    List<FiscalObligation> findByStatusAndVencimentoBetween(
            ObligationStatus status, LocalDate inicio, LocalDate fim);
}
