package com.logicalastrology.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_signo_analise")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignoAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String signo;

    @Column(name = "data_analise")
    private LocalDate dataAnalise;

    @Column(length = 2000)
    private String resumo;

    private String sentimento;

    private double coerencia;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_signo_analise_highlight", joinColumns = @JoinColumn(name = "analise_id"))
    @Column(name = "descricao")
    private List<String> destaques = new ArrayList<>();

    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
