package com.ledgerflow.modules.apikey.service;

import com.ledgerflow.modules.apikey.entity.ApiKey;
import com.ledgerflow.modules.apikey.repository.ApiKeyRepository;
import com.ledgerflow.security.SecurityUtils;
import com.ledgerflow.shared.exception.ResourceNotFoundException;
import com.ledgerflow.shared.util.HashUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository repository;

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    public CreatedApiKey create(String nome, String escopos) {
        String rawKey = "lf_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        ApiKey apiKey = new ApiKey();
        apiKey.setEmpresaId(SecurityUtils.currentEmpresaId());
        apiKey.setNome(nome);
        apiKey.setEscopos(escopos);
        apiKey.setChaveHash(hash(rawKey));
        ApiKey saved = repository.save(apiKey);
        return new CreatedApiKey(saved.getId(), nome, escopos, rawKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> list() {
        return repository.findByEmpresaId(SecurityUtils.currentEmpresaId());
    }

    public void revoke(UUID id) {
        ApiKey apiKey = repository.findByIdAndEmpresaId(id, SecurityUtils.currentEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Chave nao encontrada."));
        apiKey.setAtivo(false);
        repository.save(apiKey);
    }

    public static String hash(String rawKey) {
        return HashUtil.sha256Hex(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    public record CreatedApiKey(UUID id, String nome, String escopos, String chave) {
    }
}
