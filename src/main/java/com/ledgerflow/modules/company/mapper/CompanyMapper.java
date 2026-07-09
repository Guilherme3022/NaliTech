package com.ledgerflow.modules.company.mapper;

import com.ledgerflow.modules.company.dto.CompanyDtos.CompanyResponse;
import com.ledgerflow.modules.company.entity.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyResponse toResponse(Company company);
}
