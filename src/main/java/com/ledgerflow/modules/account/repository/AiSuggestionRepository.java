package com.ledgerflow.modules.account.repository;

import com.ledgerflow.modules.account.entity.AiSuggestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
}
