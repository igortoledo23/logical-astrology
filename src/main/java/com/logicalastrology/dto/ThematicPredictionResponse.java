package com.logicalastrology.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ThematicPredictionResponse {

    private final String preferenceId;
    private final String predictionId;
    private final String initPoint;
    private final String sandboxInitPoint;
    private final LocalDateTime expiresAt;
    private final boolean discountApplied;
    private final BigDecimal valorBase;
    private final BigDecimal valorFinal;
    private final String status;
    private final String publicKey;
}
