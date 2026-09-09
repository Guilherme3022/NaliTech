package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.shared.util.DescriptionNormalizer;
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
        recordDecision(empresaId, clienteId, descricao, null, contaId);
    }

    /**
     * Registra a decisao do contador para retroalimentar a sugestao. Aprende por DOIS
     * criterios quando possivel:
     *   - <b>CNPJ/CPF</b> da contraparte (chave exata "#digitos") — mais confiavel;
     *   - <b>nome/descricao</b> normalizada (fallback por similaridade).
     * Assim o mesmo fornecedor e reconhecido mesmo com pequenas variacoes de grafia.
     */
    public void recordDecision(UUID empresaId, UUID clienteId, String descricao, String documento,
                               UUID contaId) {
        if (contaId == null) {
            return;
        }
        if (descricao != null && !descricao.isBlank()) {
            String padrao = DescriptionNormalizer.normalize(descricao);
            if (!padrao.isBlank()) {
                recordPattern(empresaId, clienteId, padrao, contaId);
            }
        }
        String docKey = documentoKey(documento);
        if (docKey != null) {
            recordPattern(empresaId, clienteId, docKey, contaId);
        }
    }

    /** Chave de aprendizado por CNPJ/CPF (>=11 digitos): "#<digitos>", ou null. */
    public static String documentoKey(String documento) {
        if (documento == null) {
            return null;
        }
        String digitos = documento.replaceAll("\\D", "");
        return digitos.length() >= 11 ? "#" + digitos : null;
    }

    private void recordPattern(UUID empresaId, UUID clienteId, String padrao, UUID contaId) {
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

}
