package com.nalitech.modules.account.ai;

import com.nalitech.modules.account.entity.ChartOfAccount;
import com.nalitech.modules.movement.entity.Movement;
import com.nalitech.shared.util.DescriptionNormalizer;
import com.nalitech.shared.util.StringSimilarity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Provedor deterministico que sugere a conta casando a <b>descricao da movimentacao</b>
 * contra o <b>nome das contas do plano do cliente</b> (ja filtrado por escopo pelo
 * {@link com.nalitech.modules.account.service.ClassificationSuggestionService}).
 *
 * <p>Complementa o {@link HeuristicSuggestionProvider}, que so casa contra o historico
 * de classificacoes passadas: cliente novo, sem historico, passa a receber sugestao a
 * partir do proprio plano (ex.: deposito "BLACK AND DECKER" -> conta "BLACK AND DECKER").
 * Custo zero, sem IA externa.</p>
 */
@Component
public class ChartNameSuggestionProvider implements AiSuggestionProvider {

    // Coeficiente de sobreposicao minimo para aceitar (|comuns| / min(tokens)).
    private static final double THRESHOLD = 0.6;

    @Override
    public String origem() {
        return "PLANO_CONTAS";
    }

    @Override
    public Optional<SuggestedAccount> suggest(Movement movement, List<ChartOfAccount> contas) {
        if (contas == null || contas.isEmpty()) {
            return Optional.empty();
        }
        String alvo = DescriptionNormalizer.normalize(movement.getDescricao());
        if (alvo.isBlank()) {
            return Optional.empty();
        }
        int alvoTokens = StringSimilarity.tokenCount(alvo);

        ChartOfAccount melhor = null;
        double melhorNota = 0.0;
        for (ChartOfAccount conta : contas) {
            String nome = DescriptionNormalizer.normalize(conta.getNome());
            if (nome.isBlank()) {
                continue;
            }
            int comuns = StringSimilarity.commonTokenCount(alvo, nome);
            if (comuns == 0) {
                continue;
            }
            int menorLado = Math.min(alvoTokens, StringSimilarity.tokenCount(nome));
            // Guarda anti-falso-positivo: exige 2+ tokens distintivos em comum, ou entao
            // que um dos lados seja um unico token (ex.: conta "NESTLE" x "pix nestle").
            boolean forte = comuns >= 2 || menorLado == 1;
            if (!forte) {
                continue;
            }
            // Coeficiente de sobreposicao: nao penaliza a razao social ter palavras extras.
            double nota = StringSimilarity.tokenOverlap(alvo, nome);
            if (nota > melhorNota) {
                melhorNota = nota;
                melhor = conta;
            }
        }
        if (melhor == null || melhorNota < THRESHOLD) {
            return Optional.empty();
        }
        // Mapeia a nota (0.6..1.0) para uma confianca de 60..90.
        int confianca = (int) Math.round(Math.min(90, 60 + (melhorNota - THRESHOLD) * 75));
        return Optional.of(new SuggestedAccount(melhor.getId(), BigDecimal.valueOf(confianca)));
    }
}
