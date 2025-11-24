package com.logicalastrology.controller;

import com.logicalastrology.dto.ThematicPredictionRequest;
import com.logicalastrology.dto.ThematicPredictionResponse;
import com.logicalastrology.dto.ThematicPredictionStatusDTO;
import com.logicalastrology.service.ThematicPredictionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ThematicPredictionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThematicPredictionController.class);

    private final ThematicPredictionService predictionService;

    public ThematicPredictionController(ThematicPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/previsoes/tematicas")
    public ResponseEntity<ThematicPredictionResponse> criar(@Valid @RequestBody ThematicPredictionRequest request) {
        LOGGER.info("Solicitando previsão temática para tema {}", request.getTema());
        try {
            return ResponseEntity.ok(predictionService.criarPrevisao(request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/previsoes/tematicas/{preferenceId}")
    public ResponseEntity<ThematicPredictionStatusDTO> status(@PathVariable String preferenceId) {
        LOGGER.info("Consultando status da previsão temática {}", preferenceId);
        try {
            return ResponseEntity.ok(predictionService.buscarStatus(preferenceId));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/pagamentos/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(value = "data.id", required = false) String dataId,
                                        @RequestParam(value = "id", required = false) String queryId,
                                        @RequestBody(required = false) Map<String, Object> payload) {
        String paymentId = extractPaymentId(dataId, queryId, payload);
        if (paymentId != null) {
            LOGGER.info("Webhook de pagamento recebido, id={}", paymentId);
            predictionService.registrarNotificacaoPagamento(paymentId);
        } else {
            LOGGER.warn("Webhook de pagamento recebido sem id identificável: {}", payload);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private String extractPaymentId(String dataId, String queryId, Map<String, Object> payload) {
        if (dataId != null && !dataId.isBlank()) {
            return dataId;
        }
        if (queryId != null && !queryId.isBlank()) {
            return queryId;
        }
        if (payload != null) {
            Object data = payload.get("data");
            if (data instanceof Map<?, ?> map) {
                Object nestedId = map.get("id");
                if (nestedId != null) {
                    return nestedId.toString();
                }
            }
            Object id = payload.get("id");
            if (id != null) {
                return id.toString();
            }
        }
        return null;
    }
}
