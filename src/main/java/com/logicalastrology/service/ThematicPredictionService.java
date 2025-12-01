package com.logicalastrology.service;

import com.logicalastrology.dto.ThematicPredictionRequest;
import com.logicalastrology.dto.ThematicPredictionResponse;
import com.logicalastrology.dto.ThematicPredictionStatusDTO;
import com.logicalastrology.model.PredictionSentiment;
import com.logicalastrology.model.PredictionStatus;
import com.logicalastrology.model.PredictionTheme;
import com.logicalastrology.model.ThemedPrediction;
import com.logicalastrology.payment.MercadoPagoClient;
import com.logicalastrology.repository.ThemedPredictionRepository;
import com.logicalastrology.nlp.NlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThematicPredictionService {

    @Value("${mercadopago.default-payment-value}")
    private BigDecimal VALOR_BASE;
    @Value("${mercadopago.public-key}")
    private String PUBLIC_KEY;
    private static final BigDecimal DESCONTO = new BigDecimal("0.30");
    private static final Duration VALIDADE_TOKEN = Duration.ofMinutes(30);

    private final ThemedPredictionRepository repository;
    private final MercadoPagoClient mercadoPagoClient;
    private final NlpService nlpService;

    @Transactional
    public ThematicPredictionResponse criarPrevisao(ThematicPredictionRequest request) {
        PredictionTheme tema = PredictionTheme.fromString(request.getTema());
        PredictionSentiment sentimento = PredictionSentiment.fromString(request.getSentimento());
        if (tema == null || sentimento == null) {
            throw new IllegalArgumentException("Tema ou sentimento inválido");
        }

        LocalDateTime now = LocalDateTime.now();

        // 🔹 1) Tenta reusar uma previsão pendente vinculada ao activePaymentToken
        ThemedPrediction predictionReusada = null;
        String activeToken = request.getActivePaymentToken();

        if (StringUtils.hasText(activeToken)) {
            predictionReusada = repository.findByPreferenceId(activeToken)
                    .filter(p -> p.getStatus() == PredictionStatus.PENDING_PAYMENT)
                    .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(now))
                    .orElse(null);
        }


        boolean desconto = possuiTokenAtivo(request.getActivePaymentToken());
        BigDecimal valorFinal = calcularValor(desconto);
        LocalDateTime expiraEm = now.plus(Duration.ofMinutes(1440));

        if (predictionReusada != null) {
            log.info("Reutilizando previsão temática pendente. preferenceId={}",
                    predictionReusada.getPreferenceId());

            // aqui você pode decidir: usa o valor original ou o recalculado
            // vou manter o que já estava gravado na previsão (mais consistente)
            return ThematicPredictionResponse.builder()
                    .preferenceId(predictionReusada.getPreferenceId())
                    .predictionId(Optional.ofNullable(predictionReusada.getId()).map(UUID::toString).orElse(null))
                    .initPoint(predictionReusada.getInitPoint())
                    .sandboxInitPoint(predictionReusada.getSandboxInitPoint())
                    .expiresAt(predictionReusada.getExpiresAt())
                    .discountApplied(predictionReusada.isDescontoAplicado())
                    .valorBase(predictionReusada.getValorBase())
                    .valorFinal(predictionReusada.getValorFinal())
                    .status(predictionReusada.getStatus().name())
                    .publicKey(PUBLIC_KEY)
                    .build();
        }

        ThemedPrediction prediction = new ThemedPrediction();
        prediction.setTema(tema);
        prediction.setSentimento(sentimento);
        prediction.setNomeUsuario(request.getNome().trim());
        prediction.setNomePar(StringUtils.hasText(request.getNomeAmor()) ? request.getNomeAmor().trim() : null);
        prediction.setStatus(PredictionStatus.PENDING_PAYMENT);
        prediction.setDescontoAplicado(desconto);
        prediction.setValorBase(VALOR_BASE);
        prediction.setValorFinal(valorFinal);
        prediction.setExpiresAt(expiraEm);

        MercadoPagoClient.PreferenceResponse preference = mercadoPagoClient.criarPreferencia(
                "Previsão " + tema.name().toLowerCase(),
                valorFinal,
                expiraEm
        );

        if (preference == null || !StringUtils.hasText(preference.getId())) {
            throw new IllegalStateException("Não foi possível criar a preferência de pagamento");
        }

        prediction.setPreferenceId(preference.getId());
        prediction.setInitPoint(preference.getInitPoint());
        prediction.setSandboxInitPoint(preference.getSandboxInitPoint());

        ThemedPrediction saved = repository.save(prediction);

        return ThematicPredictionResponse.builder()
                .preferenceId(saved.getPreferenceId())
                .predictionId(Optional.ofNullable(saved.getId()).map(UUID::toString).orElse(null))
                .initPoint(saved.getInitPoint())
                .sandboxInitPoint(saved.getSandboxInitPoint())
                .expiresAt(saved.getExpiresAt())
                .discountApplied(saved.isDescontoAplicado())
                .valorBase(saved.getValorBase())
                .valorFinal(saved.getValorFinal())
                .status(saved.getStatus().name())
                .publicKey(preference.getPublicKey())
                .build();
    }

    @Transactional
    public ThematicPredictionStatusDTO buscarStatus(String preferenceId) {
        ThemedPrediction prediction = repository.findByPreferenceId(preferenceId)
                .orElseThrow(() -> new IllegalArgumentException("Previsão não encontrada"));

        if (prediction.getStatus() != PredictionStatus.PAID) {
            if (prediction.getExpiresAt() != null && prediction.getExpiresAt().isBefore(LocalDateTime.now())) {
                prediction.setStatus(PredictionStatus.EXPIRED);
            }
        }

        return ThematicPredictionStatusDTO.builder()
                .preferenceId(prediction.getPreferenceId())
                .status(prediction.getStatus().name())
                .expiresAt(prediction.getExpiresAt())
                .tema(prediction.getTema().name())
                .mensagem(prediction.getStatus() == PredictionStatus.PAID ? prediction.getMensagemIa() : null)
                .ativo(prediction.getStatus() == PredictionStatus.PAID && prediction.getExpiresAt() != null && prediction.getExpiresAt().isAfter(LocalDateTime.now()))
                .build();
    }

    @Transactional
    public void registrarNotificacaoPagamento(String paymentId) {
        mercadoPagoClient.extrairPreferenceIdDePagamento(paymentId)
                .flatMap(repository::findByPreferenceId)
                .ifPresent(prediction -> {
                    if (prediction.getStatus() == PredictionStatus.PAID) {
                        log.info("Pagamento já processado para preferenceId {}", prediction.getPreferenceId());
                        return;
                    }
                    if (prediction.getExpiresAt() != null && prediction.getExpiresAt().isBefore(LocalDateTime.now())) {
                        prediction.setStatus(PredictionStatus.EXPIRED);
                        log.info("Pagamento recebido após expiração para preferenceId {}", prediction.getPreferenceId());
                        return;
                    }
                    confirmarPagamento(prediction);
                    log.info("Pagamento confirmado para preferenceId {}", prediction.getPreferenceId());
                });
    }

    private void confirmarPagamento(ThemedPrediction prediction) {
        if (prediction.getStatus() == PredictionStatus.PAID) {
            return;
        }
        prediction.setStatus(PredictionStatus.PAID);
        prediction.setMensagemIa(gerarMensagemIa(prediction));
        prediction.setMensagemGeradaEm(LocalDateTime.now());
    }

    private boolean possuiTokenAtivo(String activePaymentToken) {
        if (!StringUtils.hasText(activePaymentToken)) {
            return false;
        }
        return repository.existsByPreferenceIdAndStatusAndExpiresAtAfter(
                activePaymentToken,
                PredictionStatus.PAID,
                LocalDateTime.now()
        );
    }

    private BigDecimal calcularValor(boolean descontoAplicado) {
        if (!descontoAplicado) {
            return VALOR_BASE;
        }
        return VALOR_BASE.multiply(BigDecimal.ONE.subtract(DESCONTO)).setScale(2, RoundingMode.HALF_UP);
    }

    private String gerarMensagemIa(ThemedPrediction prediction) {
        try {
            return nlpService.generateThematicPrediction(
                    prediction.getTema(),
                    prediction.getNomeUsuario(),
                    prediction.getNomePar(),
                    prediction.getSentimento()
            );
        } catch (Exception ex) {
            log.warn("Falha ao gerar mensagem temática via IA: {}", ex.getMessage());
            return fallbackMensagem(prediction);
        }
    }

    private String fallbackMensagem(ThemedPrediction prediction) {
        return "%s, esta mensagem é para trazer confiança sobre %s. Mantenha a mente %s e avance com coragem.".formatted(
                prediction.getNomeUsuario(),
                prediction.getTema().name().toLowerCase(),
                prediction.getSentimento() == PredictionSentiment.POSITIVO ? "positiva" : "serena"
        );
    }
}
