package com.logicalastrology.service;

import com.logicalastrology.dto.AnaliseComparativaDTO;
import com.logicalastrology.dto.AnaliseSignoDTO;
import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.model.SignoAnalise;
import com.logicalastrology.nlp.AiAnalysisResult;
import com.logicalastrology.nlp.NlpService;
import com.logicalastrology.repository.HoroscopoRepository;
import com.logicalastrology.repository.SignoAnaliseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AnaliseSignoService.class);
    private static final List<String> SIGNS = List.of(
            "aries", "touro", "gemeos", "cancer", "leao", "virgem", "libra", "escorpiao",
            "sagitario", "capricornio", "aquario", "peixes");

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
        return analisar(sign, null);
    }

    public AnaliseSignoDTO analisar(String sign, LocalDate data) {
        String normalized = normalize(sign);
        LocalDate dataReferencia = data == null ? LocalDate.now() : data;
        LOGGER.info("Iniciando análise para o signo {} na data {}", normalized, dataReferencia);

        Optional<SignoAnalise> existenteHoje = signoAnaliseRepository
                .findTopBySignoIgnoreCaseAndDataAnaliseOrderByCriadoEmDesc(normalized, dataReferencia);
        if (existenteHoje.isPresent() && existenteHoje.get().isGenerated()) {
            LOGGER.info("Análise encontrada para {} em {}. Reutilizando resultado existente.", normalized, dataReferencia);
            return toDto(existenteHoje.get());
        }

        SignoAnalise novaAnalise = gerarNovaAnalise(normalized, dataReferencia, existenteHoje.orElse(null));
        return toDto(novaAnalise);
    }

    public List<AnaliseSignoDTO> analisarTodos(LocalDate data) {
        LocalDate dataReferencia = data == null ? LocalDate.now() : data;
        LOGGER.info("Gerando/recuperando análises para todos os signos na data {}", dataReferencia);
        return SIGNS.stream().map(sign -> analisar(sign, dataReferencia)).toList();
    }

    public List<AnaliseComparativaDTO> obterComparativo(String sign, LocalDate data) {
        LocalDate dataReferencia = data == null ? LocalDate.now() : data;
        LOGGER.info("Montando comparativo de horóscopos e análise para {} na data {}", sign, dataReferencia);
        AnaliseSignoDTO analise = analisar(sign, dataReferencia);
        List<Horoscopo> horoscoposDoDia = buscarHoroscoposDoDia(normalize(sign), dataReferencia);
        if (horoscoposDoDia.isEmpty()) {
            horoscoposDoDia = horoscopoRepository.findTop10BySignoIgnoreCaseOrderByDataColetaDesc(normalize(sign));
        }

        String resumo = analise.getResumo() == null ? "" : analise.getResumo();
        LocalDate dataAnalise = analise.getDataAnalise();

        return horoscoposDoDia.stream()
                .map(h -> AnaliseComparativaDTO.builder()
                        .dataAnalise(dataAnalise)
                        .analise(resumo)
                        .fonte(h.getFonte())
                        .horoscopoFonte(h.getDescricao())
                        .build())
                .toList();
    }

    private SignoAnalise gerarNovaAnalise(String normalized, LocalDate data, SignoAnalise existente) {
        List<Horoscopo> horoscoposDoDia = buscarHoroscoposDoDia(normalized, data);
        if (horoscoposDoDia.isEmpty()) {
            LOGGER.info("Nenhum horóscopo encontrado para {} em {}. Buscando últimos registros.", normalized, data);
            horoscoposDoDia = horoscopoRepository.findTop10BySignoIgnoreCaseOrderByDataColetaDesc(normalized);
        }

        List<String> textos = horoscoposDoDia.stream()
                .map(Horoscopo::getDescricao)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        LOGGER.info("Enviando {} textos para IA consolidar análise de {} em {}", textos.size(), normalized, data);
        AiAnalysisResult resultado = nlpService.analyze(normalized, textos);

        String resumo = resultado.summary() == null ? "" : resultado.summary();
        String sentimento = resultado.sentiment() == null ? "Indefinido" : resultado.sentiment();
        double coerencia = resultado.coherenceScore();
        List<String> destaques = resultado.highlights() == null
                ? Collections.emptyList()
                : new ArrayList<>(resultado.highlights());

        SignoAnalise entidade = existente != null ? existente : new SignoAnalise();
        entidade.setSigno(normalized);
        entidade.setDataAnalise(data);
        entidade.setResumo(resumo);
        entidade.setSentimento(sentimento);
        entidade.setCoerencia(coerencia);
        entidade.setDestaques(new ArrayList<>(destaques));
        entidade.setGenerated(true);
        if (entidade.getCriadoEm() == null) {
            entidade.setCriadoEm(LocalDateTime.now());
        }

        SignoAnalise salvo = signoAnaliseRepository.save(entidade);
        LOGGER.info("Análise de {} para {} salva com sucesso", normalized, data);
        return salvo;
    }

    private List<Horoscopo> buscarHoroscoposDoDia(String sign, LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();
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
