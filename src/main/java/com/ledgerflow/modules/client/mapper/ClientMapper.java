package com.ledgerflow.modules.client.mapper;

import com.ledgerflow.modules.client.dto.ClientDtos.ClientDocumentResponse;
import com.ledgerflow.modules.client.dto.ClientDtos.ClientResponse;
import com.ledgerflow.modules.client.entity.Client;
import com.ledgerflow.modules.client.entity.ClientDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    ClientDocumentResponse toDocumentResponse(ClientDocument document);
}
