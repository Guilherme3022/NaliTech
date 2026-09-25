package com.nalitech.modules.account.service;

import com.nalitech.modules.account.entity.LearningHistory;
import com.nalitech.modules.account.repository.LearningHistoryRepository;
import com.nalitech.shared.util.DescriptionNormalizer;
import java.util.Objects;
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

    public void recordDecision(UUID empresaId, UUID clienteId, String descricao,
                               UUID contaDebitoId, UUID contaCreditoId) {
        recordDecision(empresaId, clienteId, descricao, null, contaDebitoId, contaCreditoId);
    }

    /**
     * Registra a decisao do contador (a PARTIDA DOBRADA: conta de debito + credito) para
     * retroalimentar a sugestao. Aprende por DOIS criterios quando possivel:
     *   - <b>CNPJ/CPF</b> da contraparte (chave exata "#digitos") — mais confiavel;
     *   - <b>nome/descricao</b> normalizada (fallback por similaridade).
     * Assim, na proxima vez, as DUAS contas ja vem preenchidas.
     */
    public void recordDecision(UUID empresaId, UUID clienteId, String descricao, String documento,
                               UUID contaDebitoId, UUID contaCreditoId) {
        if (contaDebitoId == null && contaCreditoId == null) {
            return;
        }
        if (descricao != null && !descricao.isBlank()) {
            String padrao = DescriptionNormalizer.normalize(descricao);
            if (!padrao.isBlank()) {
                recordPattern(empresaId, clienteId, padrao, contaDebitoId, contaCreditoId);
            }
        }
        String docKey = documentoKey(documento);
        if (docKey != null) {
            recordPattern(empresaId, clienteId, docKey, contaDebitoId, contaCreditoId);
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

    private void recordPattern(UUID empresaId, UUID clienteId, String padrao,
                               UUID contaDebitoId, UUID contaCreditoId) {
        LearningHistory history = learningRepository
                .findScoped(empresaId, clienteId, padrao)
                .orElseGet(() -> novo(empresaId, clienteId, padrao));

        boolean mesmoPar = history.getId() != null
                && Objects.equals(history.getContaDebitoId(), contaDebitoId)
                && Objects.equals(history.getContaCreditoId(), contaCreditoId);
        if (mesmoPar) {
            history.setOcorrencias(history.getOcorrencias() + 1);
        } else {
            history.setContaDebitoId(contaDebitoId);
            history.setContaCreditoId(contaCreditoId);
            history.setOcorrencias(1);
        }
        learningRepository.save(history);
    }

    private LearningHistory novo(UUID empresaId, UUID clienteId, String padrao) {
        LearningHistory history = new LearningHistory();
        history.setEmpresaId(empresaId);
        history.setClienteId(clienteId);
        history.setDescricaoPadrao(padrao);
        history.setOcorrencias(0);
        return history;
    }

}
