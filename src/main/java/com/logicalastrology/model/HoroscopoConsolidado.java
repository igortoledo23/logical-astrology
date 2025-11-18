package com.logicalastrology.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_horoscopo_consolidado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoroscopoConsolidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String signo;

    @Column(length = 2000)
    private String descricaoFinal;

    private String sentimentoPredominante;

    private LocalDateTime dataAnalise;
}
