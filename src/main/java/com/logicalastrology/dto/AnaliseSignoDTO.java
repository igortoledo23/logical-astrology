package com.logicalastrology.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseSignoDTO {

    private String signo;
    private LocalDate dataAnalise;
    private String resumo;
    private String sentimento;
    private double coerencia;
    @Builder.Default
    private List<String> destaques = Collections.emptyList();
}
