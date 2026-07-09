package com.ledgerflow.modules.account.service;

import com.ledgerflow.modules.account.entity.LearningHistory;
import com.ledgerflow.modules.account.repository.LearningHistoryRepository;
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

    public void recordDecision(UUID empresaId, String descricao, UUID contaId) {
        if (descricao == null || descricao.isBlank() || contaId == null) {
            return;
        }
        String padrao = normalize(descricao);
        LearningHistory history = learningRepository
                .findByEmpresaIdAndDescricaoPadrao(empresaId, padrao)
                .orElseGet(() -> novo(empresaId, padrao, contaId));

        if (history.getId() != null && history.getContaId().equals(contaId)) {
            history.setOcorrencias(history.getOcorrencias() + 1);
        } else {

            history.setContaId(contaId);
            history.setOcorrencias(1);
        }
        learningRepository.save(history);
    }

    private LearningHistory novo(UUID empresaId, String padrao, UUID contaId) {
        LearningHistory history = new LearningHistory();
        history.setEmpresaId(empresaId);
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
