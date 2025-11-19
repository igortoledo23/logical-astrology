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
        String descricaoFinal = extrairDescricaoFinal(signo, result, horoscopos);

        HoroscopoConsolidado consolidado = HoroscopoConsolidado.builder()
                .signo(signo.toLowerCase(Locale.ROOT))
                .descricaoFinal(descricaoFinal)
                .sentimentoPredominante(result.sentiment())
                .coherenceScore(result.coherenceScore())
                .dataAnalise(LocalDateTime.now())
                .build();

        return consolidadoRepository.save(consolidado);
    }

    private String extrairDescricaoFinal(String signo, AiAnalysisResult result, List<Horoscopo> fontes) {
        if (result.summary() != null && !result.summary().isBlank()) {
            return result.summary().trim();
        }

        return montarDescricaoFallback(signo, result, fontes);
    }

    private String montarDescricaoFallback(String signo, AiAnalysisResult result, List<Horoscopo> fontes) {
        String normalizedSigno = signo == null ? "" : signo.trim();
        if (normalizedSigno.isEmpty()) {
            normalizedSigno = "signo";
        }
        String displaySign = normalizedSigno.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalizedSigno.substring(1).toLowerCase(Locale.ROOT);

        List<String> destaques = result.highlights() == null ? List.of() : result.highlights().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> fontesAnalisadas = fontes.stream()
                .map(Horoscopo::getFonte)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        String sentimento = result.sentiment() == null ? "Neutro" : result.sentiment();
        String sentimentoNarrativo = switch (sentimento.toLowerCase(Locale.ROOT)) {
            case "positivo" -> "um clima otimista";
            case "negativo" -> "um alerta mais cauteloso";
            default -> "uma energia equilibrada";
        };

        StringBuilder builder = new StringBuilder();
        builder.append("Consolidação lógica para ")
                .append(displaySign)
                .append('.');

        if (!fontesAnalisadas.isEmpty()) {
            builder.append(' ')
                    .append("Cruzamos as mensagens de ")
                    .append(humanizeList(fontesAnalisadas))
                    .append(", procurando apenas o que aparece em mais de um texto.");
        } else {
            builder.append(' ')
                    .append("Cruzamos as previsões disponíveis e destacamos somente os trechos repetidos entre elas.");
        }

        builder.append(' ')
                .append("Quando duas ou mais fontes insistem em um assunto, ele vira a linha-guia da análise.");

        if (!destaques.isEmpty()) {
            builder.append(' ')
                    .append("Nesta coleta, os temas que se repetem são ")
                    .append(humanizeList(destaques))
                    .append(", formando o núcleo lógico do dia.");
        }

        String resumo = result.summary();
        if (resumo == null || resumo.isBlank()) {
            resumo = "Sem resumo disponível.";
        }
        builder.append(' ')
                .append("Resumo do cruzamento: ")
                .append(resumo);

        builder.append(' ')
                .append("O sentimento predominante é ")
                .append(sentimento.toLowerCase(Locale.ROOT))
                .append(", traduzindo ")
                .append(sentimentoNarrativo)
                .append(',')
                .append(' ')
                .append("com coerência calculada em ")
                .append(String.format(Locale.US, "%.2f", result.coherenceScore()))
                .append(',')
                .append(' ')
                .append("o que indica que as fontes concordam nesse nível de intensidade.");

        return builder.toString().trim();
    }

    private String humanizeList(List<String> itens) {
        if (itens == null || itens.isEmpty()) {
            return "";
        }
        if (itens.size() == 1) {
            return itens.get(0);
        }
        if (itens.size() == 2) {
            return itens.get(0) + " e " + itens.get(1);
        }
        return String.join(", ", itens.subList(0, itens.size() - 1))
                + " e "
                + itens.get(itens.size() - 1);
    }
}
