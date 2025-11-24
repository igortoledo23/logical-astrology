package com.logicalastrology.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ThematicPredictionStatusDTO {
    private final String preferenceId;
    private final String status;
    private final LocalDateTime expiresAt;
    private final String tema;
    private final String mensagem;
    private final boolean ativo;
}
