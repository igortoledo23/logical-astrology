package com.logicalastrology.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseComparativaDTO {
    private LocalDate dataAnalise;
    private String analise;
    private String fonte;
    private String horoscopoFonte;
}
