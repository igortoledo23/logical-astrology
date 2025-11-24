package com.logicalastrology.payment;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPElementsResourcesPage;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.merchantorder.MerchantOrderPayment;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class MercadoPagoClient {

    private final PreferenceClient preferenceClient;
    private final MerchantOrderClient merchantOrderClient;
    private final PaymentClient paymentClient;
    private final String notificationUrl;
    private final String backUrl;
    private final String publicKey;

    public MercadoPagoClient(@Value("${mercadopago.access-token}") String accessToken,
                             @Value("${mercadopago.notification-url:http://localhost:8080/api/pagamentos/webhook}") String notificationUrl,
                             @Value("${mercadopago.back-url:http://localhost:8080/}") String backUrl,
                             @Value("${mercadopago.public-key}") String publicKey) {
        this.notificationUrl = notificationUrl;
        this.backUrl = backUrl;
        this.publicKey = publicKey;
        MercadoPagoConfig.setAccessToken(accessToken);
        this.preferenceClient = new PreferenceClient();
        this.merchantOrderClient = new MerchantOrderClient();
        this.paymentClient = new PaymentClient();
    }

    public PreferenceResponse criarPreferencia(String titulo, BigDecimal valor, LocalDateTime expiraEm) {
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime fim = expiraEm.atOffset(ZoneOffset.UTC);

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(PreferenceItemRequest.builder()
                        .title(titulo)
                        .quantity(1)
                        .unitPrice(valor)
                        .currencyId("BRL")
                        .build()))
                .payer(PreferencePayerRequest.builder().name(titulo).build())
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(backUrl)
                        .failure(backUrl)
                        .pending(backUrl)
                        .build())
                .autoReturn("approved")
                .notificationUrl(notificationUrl)
                .statementDescriptor("Logical Astrology")
                .expires(true)
                .expirationDateFrom(inicio)
                .expirationDateTo(fim)
                .build();

        try {
            Preference preference = preferenceClient.create(request);
            if (preference == null) {
                log.warn("Preferência retornou nula ao criar pagamento");
                return null;
            }
            return PreferenceResponse.builder()
                    .id(preference.getId())
                    .initPoint(preference.getInitPoint())
                    .sandboxInitPoint(preference.getSandboxInitPoint())
                    .expiresAt(expiraEm)
                    .publicKey(publicKey)
                    .build();
        } catch (MPApiException | MPException ex) {
            log.warn("Erro ao criar preferência no Mercado Pago: {}", ex.getMessage());
            return null;
        }
    }

    public boolean pagamentoAprovado(String preferenceId) {
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("preference_id", preferenceId);

        try {
            MPElementsResourcesPage<MerchantOrder> resultado = merchantOrderClient.search(
                    MPSearchRequest.builder()
                            .offset(0)
                            .limit(10)
                            .filters(filtros)
                            .build()
            );

            List<MerchantOrder> ordens = resultado != null ? resultado.getElements() : null;
            if (ordens == null || ordens.isEmpty()) {
                log.warn("Nenhuma merchant order encontrada para preferência {}", preferenceId);
                return false;
            }

            for (MerchantOrder order : ordens) {
                if (order == null) {
                    continue;
                }

                if ("paid".equalsIgnoreCase(order.getOrderStatus())) {
                    return true;
                }

                List<MerchantOrderPayment> pagamentos = order.getPayments();
                if (pagamentos == null || pagamentos.isEmpty()) {
                    continue;
                }

                boolean aprovado = pagamentos.stream()
                        .anyMatch(p -> "approved".equalsIgnoreCase(p.getStatus()));

                if (aprovado) {
                    return true;
                }
            }
        } catch (MPApiException | MPException ex) {
            log.warn("Falha ao buscar status do pagamento para preferência {}: {}", preferenceId, ex.getMessage());
        }
        return false;
    }

    public Optional<String> extrairPreferenceIdDePagamento(String paymentId) {
        try {
            long paymentNumericId = Long.parseLong(paymentId);
            Payment payment = paymentClient.get(paymentNumericId);
            if (payment == null || payment.getOrder() == null || payment.getOrder().getId() == null) {
                return Optional.empty();
            }

            MerchantOrder merchantOrder = merchantOrderClient.get(payment.getOrder().getId());
            if (merchantOrder == null) {
                return Optional.empty();
            }

            String preference = merchantOrder.getPreferenceId();
            if ("approved".equalsIgnoreCase(payment.getStatus()) && preference != null) {
                return Optional.of(preference);
            }
        } catch (NumberFormatException ex) {
            log.warn("Identificador de pagamento inválido: {}", paymentId);
        } catch (MPApiException | MPException ex) {
            log.warn("Não foi possível consultar o pagamento {}: {}", paymentId, ex.getMessage());
        }
        return Optional.empty();
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
