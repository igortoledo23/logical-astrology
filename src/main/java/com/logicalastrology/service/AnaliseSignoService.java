package com.logicalastrology.service;

import com.logicalastrology.dto.AnaliseSignoDTO;
import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.model.SignoAnalise;
import com.logicalastrology.nlp.AiAnalysisResult;
import com.logicalastrology.nlp.NlpService;
import com.logicalastrology.repository.HoroscopoRepository;
import com.logicalastrology.repository.SignoAnaliseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AnaliseSignoService {

    private final HoroscopoRepository horoscopoRepository;
    private final SignoAnaliseRepository signoAnaliseRepository;
    private final NlpService nlpService;

    public AnaliseSignoService(HoroscopoRepository horoscopoRepository,
                               SignoAnaliseRepository signoAnaliseRepository,
                               NlpService nlpService) {
        this.horoscopoRepository = horoscopoRepository;
        this.signoAnaliseRepository = signoAnaliseRepository;
        this.nlpService = nlpService;
    }

    public AnaliseSignoDTO analisar(String sign) {
        String normalized = normalize(sign);
        LocalDate hoje = LocalDate.now();

        Optional<SignoAnalise> existenteHoje = signoAnaliseRepository
                .findTopBySignoIgnoreCaseAndDataAnaliseOrderByCriadoEmDesc(normalized, hoje);
        if (existenteHoje.isPresent()) {
            return toDto(existenteHoje.get());
        }

        SignoAnalise novaAnalise = gerarNovaAnalise(normalized, hoje);
        return toDto(novaAnalise);
    }

    private SignoAnalise gerarNovaAnalise(String normalized, LocalDate hoje) {
        List<Horoscopo> horoscoposDoDia = buscarHoroscoposDoDia(normalized, hoje);
        if (horoscoposDoDia.isEmpty()) {
            horoscoposDoDia = horoscopoRepository.findTop10BySignoIgnoreCaseOrderByDataColetaDesc(normalized);
        }

        List<String> textos = horoscoposDoDia.stream()
                .map(Horoscopo::getDescricao)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        AiAnalysisResult resultado = nlpService.analyze(normalized, textos);

        String resumo = resultado.summary() == null ? "" : resultado.summary();
        String sentimento = resultado.sentiment() == null ? "Indefinido" : resultado.sentiment();
        double coerencia = resultado.coherenceScore();
        List<String> destaques = resultado.highlights() == null
                ? Collections.emptyList()
                : new ArrayList<>(resultado.highlights());

        SignoAnalise entidade = SignoAnalise.builder()
                .signo(normalized)
                .dataAnalise(hoje)
                .resumo(resumo)
                .sentimento(sentimento)
                .coerencia(coerencia)
                .destaques(destaques)
                .build();

        return signoAnaliseRepository.save(entidade);
    }

    private List<Horoscopo> buscarHoroscoposDoDia(String sign, LocalDate hoje) {
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay();
        return horoscopoRepository.findBySignoIgnoreCaseAndDataColetaBetween(sign, inicio, fim);
    }

    private AnaliseSignoDTO toDto(SignoAnalise analise) {
        List<String> destaques = analise.getDestaques() == null ? Collections.emptyList() : analise.getDestaques();
        return AnaliseSignoDTO.builder()
                .signo(analise.getSigno())
                .dataAnalise(analise.getDataAnalise())
                .resumo(analise.getResumo())
                .sentimento(analise.getSentimento())
                .coerencia(analise.getCoerencia())
                .destaques(destaques)
                .build();
    }

    private String normalize(String sign) {
        return sign == null ? "" : sign.trim().toLowerCase(Locale.ROOT);
    }
}
