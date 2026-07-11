package com.nalitech.modules.account.repository;

import com.nalitech.modules.account.entity.AiSuggestion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
}
