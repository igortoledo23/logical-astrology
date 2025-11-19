package com.logicalastrology.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logicalastrology.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NlpService {

    private static final String SYSTEM_PROMPT = "Você é um analista astrológico lógico. Gere um resumo único, identifique o sentimento predominante e produza uma pontuação de coerência entre 0 e 1 com base nas previsões fornecidas.";

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public NlpService(AiProperties aiProperties,
                      RestTemplateBuilder restTemplateBuilder,
                      ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        Duration timeout = aiProperties.getTimeout() == null ? Duration.ofSeconds(30) : aiProperties.getTimeout();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResult analyze(String signo, List<String> textos) {
        List<String> cleanedTexts = textos == null ? List.of() : textos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (cleanedTexts.isEmpty()) {
            return new AiAnalysisResult("Nenhuma previsão disponível para análise.", "Indefinido", 0.0, List.of());
        }

        if (shouldUseAi()) {
            try {
                AiAnalysisResult result = callAi(signo, cleanedTexts);
                if (result != null) {
                    return result;
                }
            } catch (Exception ex) {
                log.warn("Falha ao consultar IA para {}: {}", signo, ex.getMessage());
            }
        }

        return fallbackAnalysis(signo, cleanedTexts);
    }

    private boolean shouldUseAi() {
        return aiProperties.isEnabled()
                && StringUtils.hasText(aiProperties.getEndpoint())
                && StringUtils.hasText(aiProperties.getModel());
    }

    private AiAnalysisResult callAi(String signo, List<String> textos) throws RestClientException {
        Map<String, Object> body = buildRequestBody(signo, textos);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(aiProperties.getApiKey())) {
            headers.setBearerAuth(aiProperties.getApiKey());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                aiProperties.getEndpoint(),
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Resposta inválida da IA: {}", response.getStatusCode());
            return null;
        }

        return extractResultFromResponse(response.getBody());
    }

    private Map<String, Object> buildRequestBody(String signo, List<String> textos) {
        String prompt = buildPrompt(signo, textos);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProperties.getModel());
        payload.put("temperature", aiProperties.getTemperature());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
        ));
        return payload;
    }

    private String buildPrompt(String signo, List<String> textos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Junte estes textos para o signo de ")
                .append(signo)
                .append(". Junte estes dois textos e faça um resumo dos dois transformando em um só, destacando os pontos em que os dois textos deram mais evidência. Utilize linguagem humanizada e palavras fáceis. O resumo final deve estar pronto para ser exibido ao usuário e ficará no campo descricaoFinal.\n\n")
                .append("Retorne somente um JSON com o formato: {\"summary\": string, \"sentiment\": string (Positivo, Neutro ou Negativo), \"coherence\": number entre 0 e 1, \"highlights\": [string,...]}. O campo summary deve conter exatamente o texto humanizado pedido acima.\n\n");
        for (int i = 0; i < textos.size(); i++) {
            sb.append("Fonte ").append(i + 1).append(": ").append(textos.get(i)).append("\n\n");
        }
        sb.append("Certifique-se de que a resposta seja somente o JSON.");
        return sb.toString();
    }

    private AiAnalysisResult extractResultFromResponse(JsonNode body) {
        JsonNode messageNode = body.path("choices").path(0).path("message").path("content");
        if (messageNode.isMissingNode() || messageNode.asText().isBlank()) {
            return tryDirectJson(body);
        }

        try {
            return parseAiPayload(messageNode.asText());
        } catch (Exception ex) {
            log.warn("Não foi possível interpretar a resposta da IA: {}", ex.getMessage());
            return null;
        }
    }

    private AiAnalysisResult tryDirectJson(JsonNode body) {
        try {
            return parseAiPayload(objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            return null;
        }
    }

    private AiAnalysisResult parseAiPayload(String content) throws Exception {
        JsonNode json = objectMapper.readTree(content);
        String summary = json.path("summary").asText(null);
        if (!StringUtils.hasText(summary)) {
            return null;
        }
        String sentiment = json.path("sentiment").asText("Neutro");
        double coherence = json.path("coherence").asDouble(0.7);
        List<String> highlights = new ArrayList<>();
        if (json.path("highlights").isArray()) {
            json.path("highlights").forEach(node -> {
                if (StringUtils.hasText(node.asText())) {
                    highlights.add(node.asText());
                }
            });
        }
        return new AiAnalysisResult(summary, sentiment, coherence, highlights);
    }

    private AiAnalysisResult fallbackAnalysis(String signo, List<String> textos) {
        String textoCombinado = textos.stream().collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-záéíóúàâêôçãõ\\s]", " ");
        String[] palavras = textoCombinado.split("\\s+");

        Map<String, Long> frequencia = Arrays.stream(palavras)
                .filter(p -> p.length() > 3)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        List<String> topPalavras = frequencia.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        long positivas = Arrays.stream(palavras)
                .filter(p -> p.matches("feliz|alegria|sorte|sucesso|amor|prazer|otimismo|boas|vibes"))
                .count();
        long negativas = Arrays.stream(palavras)
                .filter(p -> p.matches("problema|tensão|cuidado|risco|triste|evite|difícil"))
                .count();

        String sentimento = positivas > negativas ? "Positivo" : negativas > positivas ? "Negativo" : "Neutro";
        double coerencia = Math.min(1.0, Math.max(0.1, (double) topPalavras.size() / 5.0));
        String resumo = topPalavras.isEmpty()
                ? "As previsões apresentam perspectivas variadas, sugerindo reflexão e equilíbrio."
                : String.format("As fontes convergem em temas como %s para o signo de %s.",
                String.join(", ", topPalavras),
                signo.toLowerCase());

        return new AiAnalysisResult(resumo, sentimento, coerencia, topPalavras);
    }
}
