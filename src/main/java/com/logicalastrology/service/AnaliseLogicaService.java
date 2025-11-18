package com.logicalastrology.service;

import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.model.HoroscopoConsolidado;
import com.logicalastrology.repository.HoroscopoConsolidadoRepository;
import com.logicalastrology.repository.HoroscopoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnaliseLogicaService {

    private final HoroscopoRepository horoscopoRepository;
    private final HoroscopoConsolidadoRepository consolidadoRepository;

    private static final Set<String> STOPWORDS = Set.of(
            "de", "a", "o", "e", "do", "da", "que", "em", "um", "para", "com", "no", "na",
            "os", "as", "por", "mais", "uma", "ao", "se", "como", "sua", "seu", "suas", "seus",
            "tem", "ser", "vai", "hoje", "dia", "é"
    );

    public AnaliseLogicaService(HoroscopoRepository horoscopoRepository,
                                HoroscopoConsolidadoRepository consolidadoRepository) {
        this.horoscopoRepository = horoscopoRepository;
        this.consolidadoRepository = consolidadoRepository;
    }

    public HoroscopoConsolidado analisarSigno(String signo) {
        List<Horoscopo> horoscopos = horoscopoRepository.findAll()
                .stream()
                .filter(h -> h.getSigno().equalsIgnoreCase(signo))
                .toList();

        if (horoscopos.isEmpty()) {
            throw new RuntimeException("Nenhum horóscopo encontrado para o signo: " + signo);
        }

        // Junta todos os textos
        String textoCombinado = horoscopos.stream()
                .map(Horoscopo::getDescricao)
                .collect(Collectors.joining(" "));

        // Quebra em palavras
        String[] palavras = textoCombinado.toLowerCase().replaceAll("[^a-záéíóúàâêôçãõ\\s]", "").split("\\s+");

        // Conta frequência das palavras (sem stopwords)
        Map<String, Long> frequencia = Arrays.stream(palavras)
                .filter(p -> p.length() > 3 && !STOPWORDS.contains(p))
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        // Pega as 10 palavras mais comuns
        List<String> topPalavras = frequencia.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        // Análise simples de sentimento (por palavras positivas/negativas)
        long positivas = Arrays.stream(palavras)
                .filter(p -> p.matches("feliz|alegria|sorte|sucesso|amor|prazer|otimismo|boas|vibes"))
                .count();

        long negativas = Arrays.stream(palavras)
                .filter(p -> p.matches("problema|tensão|cuidado|risco|triste|evite|difícil"))
                .count();

        String sentimento = positivas > negativas ? "Positivo" :
                negativas > positivas ? "Negativo" : "Neutro";

        // Monta uma descrição final baseada nos temas principais
        String descricaoFinal = String.format(
                "Análise consolidada para %s: temas mais frequentes envolvem %s. O sentimento predominante do dia é %s.",
                signo.substring(0, 1).toUpperCase() + signo.substring(1),
                String.join(", ", topPalavras),
                sentimento.toLowerCase()
        );

        // Salva no banco
        HoroscopoConsolidado consolidado = HoroscopoConsolidado.builder()
                .signo(signo)
                .descricaoFinal(descricaoFinal)
                .sentimentoPredominante(sentimento)
                .dataAnalise(LocalDateTime.now())
                .build();

        consolidadoRepository.save(consolidado);
        return consolidado;
    }
}
