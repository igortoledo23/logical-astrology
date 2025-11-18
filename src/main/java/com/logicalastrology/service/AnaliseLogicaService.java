package com.logicalastrology.service;

import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.model.HoroscopoConsolidado;
import com.logicalastrology.nlp.AiAnalysisResult;
import com.logicalastrology.nlp.NlpService;
import com.logicalastrology.repository.HoroscopoConsolidadoRepository;
import com.logicalastrology.repository.HoroscopoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AnaliseLogicaService {

    private final HoroscopoRepository horoscopoRepository;
    private final HoroscopoConsolidadoRepository consolidadoRepository;
    private final NlpService nlpService;

    public AnaliseLogicaService(HoroscopoRepository horoscopoRepository,
                                HoroscopoConsolidadoRepository consolidadoRepository,
                                NlpService nlpService) {
        this.horoscopoRepository = horoscopoRepository;
        this.consolidadoRepository = consolidadoRepository;
        this.nlpService = nlpService;
    }

    public HoroscopoConsolidado analisarSigno(String signo) {
        List<Horoscopo> horoscopos = horoscopoRepository.findAll().stream()
                .filter(h -> h.getSigno() != null && h.getSigno().equalsIgnoreCase(signo))
                .toList();

        if (horoscopos.isEmpty()) {
            throw new RuntimeException("Nenhum horóscopo encontrado para o signo: " + signo);
        }

        List<String> textos = horoscopos.stream()
                .map(Horoscopo::getDescricao)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        AiAnalysisResult result = nlpService.analyze(signo, textos);
        String descricaoFinal = montarDescricao(signo, result, horoscopos);

        HoroscopoConsolidado consolidado = HoroscopoConsolidado.builder()
                .signo(signo.toLowerCase(Locale.ROOT))
                .descricaoFinal(descricaoFinal)
                .sentimentoPredominante(result.sentiment())
                .coherenceScore(result.coherenceScore())
                .dataAnalise(LocalDateTime.now())
                .build();

        return consolidadoRepository.save(consolidado);
    }

    private String montarDescricao(String signo, AiAnalysisResult result, List<Horoscopo> fontes) {
        String normalizedSigno = signo == null ? "" : signo.trim();
        if (normalizedSigno.isEmpty()) {
            normalizedSigno = "signo";
        }
        String displaySign = normalizedSigno.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalizedSigno.substring(1).toLowerCase(Locale.ROOT);

        StringBuilder builder = new StringBuilder();
        builder.append("Consolidação lógica para ")
                .append(displaySign)
                .append(": ");
        builder.append(result.summary());
        if (!result.highlights().isEmpty()) {
            builder.append(" Temas recorrentes: ")
                    .append(String.join(", ", result.highlights()))
                    .append('.');
        }
        builder.append(' ')
                .append("Sentimento predominante: ")
                .append(result.sentiment().toLowerCase(Locale.ROOT))
                .append(". Coerência estimada: ")
                .append(String.format(Locale.US, "%.2f", result.coherenceScore()));
        builder.append(". Fontes analisadas: ")
                .append(fontes.stream().map(Horoscopo::getFonte).filter(Objects::nonNull).distinct().collect(Collectors.joining(", ")));
        return builder.toString().trim();
    }
}
