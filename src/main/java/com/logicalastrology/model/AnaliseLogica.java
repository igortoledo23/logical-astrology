package com.logicalastrology.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "tb_analise_logica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseLogica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String signo;
    private LocalDate data;
    private Double coherenceScore;

    @Column(columnDefinition = "TEXT")
    private String summary;
}
