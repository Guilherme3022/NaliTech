package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.AiSuggestion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {

    // Sugestao mais recente de conta para uma movimentacao (para exibir no item de conciliacao).
    Optional<AiSuggestion> findFirstByMovementIdOrderByCreatedAtDesc(UUID movementId);

    // Batch (evita N+1): sugestoes de varias movimentacoes, mais recentes primeiro. O chamador
    // pega a primeira por movementId para ter a mais recente de cada.
    List<AiSuggestion> findByMovementIdInOrderByCreatedAtDesc(Collection<UUID> movementIds);
}
