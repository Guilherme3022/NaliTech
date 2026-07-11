package com.nalitech.modules.client.mapper;

import com.nalitech.modules.client.dto.ClientDtos.ClientDocumentResponse;
import com.nalitech.modules.client.dto.ClientDtos.ClientResponse;
import com.nalitech.modules.client.entity.Client;
import com.nalitech.modules.client.entity.ClientDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    ClientDocumentResponse toDocumentResponse(ClientDocument document);
}
