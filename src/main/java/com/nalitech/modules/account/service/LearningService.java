package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LearningService {

    private final LearningHistoryRepository learningRepository;

    public LearningService(LearningHistoryRepository learningRepository) {
        this.learningRepository = learningRepository;
    }

    public void recordDecision(UUID empresaId, UUID clienteId, String descricao, UUID contaId) {
        if (descricao == null || descricao.isBlank() || contaId == null) {
            return;
        }
        String padrao = normalize(descricao);
        LearningHistory history = learningRepository
                .findScoped(empresaId, clienteId, padrao)
                .orElseGet(() -> novo(empresaId, clienteId, padrao, contaId));

        if (history.getId() != null && history.getContaId().equals(contaId)) {
            history.setOcorrencias(history.getOcorrencias() + 1);
        } else {

            history.setContaId(contaId);
            history.setOcorrencias(1);
        }
        learningRepository.save(history);
    }

    private LearningHistory novo(UUID empresaId, UUID clienteId, String padrao, UUID contaId) {
        LearningHistory history = new LearningHistory();
        history.setEmpresaId(empresaId);
        history.setClienteId(clienteId);
        history.setDescricaoPadrao(padrao);
        history.setContaId(contaId);
        history.setOcorrencias(0);
        return history;
    }

    private String normalize(String descricao) {
        String padrao = descricao.toLowerCase().replaceAll("\\s+", " ").trim();
        return padrao.length() > 200 ? padrao.substring(0, 200) : padrao;
    }
}
