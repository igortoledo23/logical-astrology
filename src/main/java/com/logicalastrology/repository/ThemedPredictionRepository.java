package com.logicalastrology.repository;

import com.logicalastrology.model.PredictionStatus;
import com.logicalastrology.model.ThemedPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ThemedPredictionRepository extends JpaRepository<ThemedPrediction, UUID> {

    Optional<ThemedPrediction> findByPreferenceId(String preferenceId);

    boolean existsByPreferenceIdAndStatusAndExpiresAtAfter(String preferenceId, PredictionStatus status, LocalDateTime now);
}
