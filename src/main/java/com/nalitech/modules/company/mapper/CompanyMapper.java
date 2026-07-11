package com.nalitech.modules.company.mapper;

import com.nalitech.modules.company.dto.CompanyDtos.CompanyResponse;
import com.nalitech.modules.company.entity.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyResponse toResponse(Company company);
}
