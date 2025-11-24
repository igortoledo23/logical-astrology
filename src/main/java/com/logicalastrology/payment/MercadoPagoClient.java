package com.logicalastrology.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class MercadoPagoClient {

    private static final String API_BASE = "https://api.mercadopago.com";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final RestTemplate restTemplate;
    private final String accessToken;
    private final String notificationUrl;
    private final String backUrl;
    private final String publicKey;

    public MercadoPagoClient(@Value("${mercadopago.access-token}") String accessToken,
                             @Value("${mercadopago.notification-url:http://localhost:8080/api/pagamentos/webhook}") String notificationUrl,
                             @Value("${mercadopago.back-url:http://localhost:8080/}") String backUrl,
                             @Value("${mercadopago.public-key}") String publicKey,
                             RestTemplateBuilder restTemplateBuilder) {
        this.accessToken = accessToken;
        this.notificationUrl = notificationUrl;
        this.backUrl = backUrl;
        this.publicKey = publicKey;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    public PreferenceResponse criarPreferencia(String titulo, BigDecimal valor, LocalDateTime expiraEm) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of(
                "title", titulo,
                "quantity", 1,
                "unit_price", valor,
                "currency_id", "BRL"
        )));
        body.put("payer", Map.of("name", titulo));
        body.put("back_urls", Map.of(
                "success", backUrl,
                "failure", backUrl,
                "pending", backUrl
        ));
        body.put("auto_return", "approved");
        body.put("notification_url", notificationUrl);
        body.put("statement_descriptor", "Logical Astrology");
        body.put("expires", true);
        ZonedDateTime inicio = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime fim = expiraEm.atZone(ZoneOffset.UTC);
        body.put("expiration_date_from", ISO_FORMATTER.format(inicio));
        body.put("expiration_date_to", ISO_FORMATTER.format(fim));

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_BASE + "/checkout/preferences",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Resposta vazia ao criar preferência no Mercado Pago");
                return null;
            }
            return PreferenceResponse.builder()
                    .id(asString(responseBody.get("id")))
                    .initPoint(asString(responseBody.get("init_point")))
                    .sandboxInitPoint(asString(responseBody.get("sandbox_init_point")))
                    .expiresAt(expiraEm)
                    .publicKey(publicKey)
                    .build();
        } catch (Exception ex) {
            log.warn("Erro ao criar preferência no Mercado Pago: {}", ex.getMessage());
            return null;
        }
    }

    public boolean pagamentoAprovado(String preferenceId) {
        HttpHeaders headers = buildHeaders();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_BASE + "/merchant_orders?pref_id=" + preferenceId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.get("elements") == null) {
                log.warn("Resposta inesperada ao verificar merchant_order para preferência {}", preferenceId);
                return false;
            }

            List<?> elements = (List<?>) body.get("elements");
            if (elements == null || elements.isEmpty()) {
                return false;
            }

            for (Object element : elements) {
                if (!(element instanceof Map<?, ?> map)) {
                    continue;
                }

                String orderStatus = asString(map.get("order_status"));
                if ("paid".equalsIgnoreCase(orderStatus)) {
                    return true;
                }

                Object paymentsObj = map.get("payments");
                if (paymentsObj instanceof List<?> payments) {
                    boolean approved = payments.stream()
                            .filter(p -> p instanceof Map<?, ?>)
                            .map(p -> (Map<?, ?>) p)
                            .anyMatch(p -> "approved".equalsIgnoreCase(asString(p.get("status"))));
                    if (approved) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Falha ao buscar status do pagamento para preferência {}: {}", preferenceId, ex.getMessage());
        }
        return false;
    }

    public Optional<String> extrairPreferenceIdDePagamento(String paymentId) {
        HttpHeaders headers = buildHeaders();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_BASE + "/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            if (body == null) {
                return Optional.empty();
            }
            String status = asString(body.get("status"));
            String preferenceId = asString(body.get("preference_id"));
            if ("approved".equalsIgnoreCase(status) && preferenceId != null) {
                return Optional.of(preferenceId);
            }
        } catch (Exception ex) {
            log.warn("Não foi possível consultar o pagamento {}: {}", paymentId, ex.getMessage());
        }
        return Optional.empty();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PreferenceResponse {
        private final String id;
        private final String initPoint;
        private final String sandboxInitPoint;
        private final LocalDateTime expiresAt;
        private final String publicKey;
    }
}
