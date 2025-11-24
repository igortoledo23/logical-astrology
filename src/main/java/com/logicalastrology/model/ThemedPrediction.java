package com.logicalastrology.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "themed_prediction", uniqueConstraints = @UniqueConstraint(columnNames = "preference_id"))
public class ThemedPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PredictionTheme tema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PredictionSentiment sentimento;

    @Column(nullable = false, length = 120)
    private String nomeUsuario;

    @Column(length = 120)
    private String nomePar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PredictionStatus status;

    @Column(name = "preference_id", length = 100, unique = true)
    private String preferenceId;

    @Column(length = 350)
    private String initPoint;

    @Column(length = 350)
    private String sandboxInitPoint;

    private LocalDateTime expiresAt;

    private boolean descontoAplicado;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorBase;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorFinal;

    @Column(length = 4096)
    private String mensagemIa;

    private LocalDateTime mensagemGeradaEm;

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void onUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
