package com.logicalastrology.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Entity
@Table(name = "tb_horoscopo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Horoscopo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String signo;
    @Column(length = 2000)
    private String descricao;
    private String fonte;
    private LocalDateTime dataColeta;

    @Column(columnDefinition = "TEXT")
    private String text;
}
