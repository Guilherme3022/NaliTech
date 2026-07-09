package com.ledgerflow.modules.company.repository;

import com.ledgerflow.modules.company.entity.Company;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByCnpj(String cnpj);
}
