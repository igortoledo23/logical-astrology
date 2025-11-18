package com.logicalastrology.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoroscopoDTO {
    private String sign;
    private String source;
    private String text;
}
