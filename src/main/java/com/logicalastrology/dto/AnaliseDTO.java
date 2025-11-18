package com.logicalastrology.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseDTO {
    private String sign;
    private Double coherenceScore;
    private String summary;
}
