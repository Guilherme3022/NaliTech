package com.ledgerflow.modules.client.service;

import com.ledgerflow.modules.client.dto.ClientDtos.ClientDocumentResponse;
import com.ledgerflow.modules.client.dto.ClientDtos.ClientResponse;
import com.ledgerflow.modules.client.dto.ClientDtos.CreateClientRequest;
import com.ledgerflow.modules.client.dto.ClientDtos.UpdateClientRequest;
import com.ledgerflow.modules.client.entity.Client;
import com.ledgerflow.modules.client.mapper.ClientMapper;
import com.ledgerflow.modules.client.repository.ClientDocumentRepository;
import com.ledgerflow.modules.client.repository.ClientRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.exception.ResourceNotFoundException;
import com.ledgerflow.shared.validation.CnpjValidator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientDocumentRepository documentRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository,
                         ClientDocumentRepository documentRepository,
                         ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.documentRepository = documentRepository;
        this.clientMapper = clientMapper;
    }

    public ClientResponse create(CreateClientRequest request) {
        Client client = new Client();
        client.setEmpresaId(SecurityUtils.currentEmpresaId());
        client.setNome(request.nome());
        client.setCnpjCpf(normalizeDocument(request.cnpjCpf()));
        client.setContato(request.contato());
        client.setTelefone(request.telefone());
        client.setEmail(request.email());
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> search(String search, Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return clientRepository.search(SecurityUtils.currentEmpresaId(), term, pageable)
                .map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(UUID id) {
        return clientMapper.toResponse(findInCurrentCompany(id));
    }

    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = findInCurrentCompany(id);
        client.setNome(request.nome());
        client.setContato(request.contato());
        client.setTelefone(request.telefone());
        client.setEmail(request.email());
        if (request.status() != null) {
            client.setStatus(request.status());
        }
        return clientMapper.toResponse(clientRepository.save(client));
    }

    public void delete(UUID id) {
        clientRepository.delete(findInCurrentCompany(id));
    }

    @Transactional(readOnly = true)
    public List<ClientDocumentResponse> listDocuments(UUID clienteId) {

        findInCurrentCompany(clienteId);
        return documentRepository
                .findByClienteIdAndEmpresaId(clienteId, SecurityUtils.currentEmpresaId())
                .stream()
                .map(clientMapper::toDocumentResponse)
                .toList();
    }

    private Client findInCurrentCompany(UUID id) {
        return clientRepository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));
    }

    private String normalizeDocument(String value) {
        return value == null ? null : CnpjValidator.normalize(value);
    }
}
