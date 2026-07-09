package com.ledgerflow.modules.account.repository;

import com.ledgerflow.modules.account.entity.LearningHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningHistoryRepository extends JpaRepository<LearningHistory, UUID> {

    Optional<LearningHistory> findByEmpresaIdAndDescricaoPadrao(UUID empresaId, String descricaoPadrao);

    List<LearningHistory> findByEmpresaId(UUID empresaId);
}
