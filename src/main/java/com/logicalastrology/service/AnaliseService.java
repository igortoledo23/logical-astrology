package com.logicalastrology.service;

import com.logicalastrology.model.AnaliseLogica;
import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.repository.AnaliseLogicaRepository;
import com.logicalastrology.repository.HoroscopoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnaliseService {

    private final HoroscopoRepository horoscopoRepository;
    private final AnaliseLogicaRepository analiseRepository;

    public AnaliseService(HoroscopoRepository horoscopoRepository, AnaliseLogicaRepository analiseRepository) {
        this.horoscopoRepository = horoscopoRepository;
        this.analiseRepository = analiseRepository;
    }

    public Optional<AnaliseLogica> findBySignAndDate(String signo, LocalDate data) {
        return analiseRepository.findBySignoAndData(signo, data);
    }

    /**
     * Analisa logicamente as previsões do dia de um signo
     * e gera um resumo e uma pontuação de coerência.
     */
    public AnaliseLogica analisarSigno(String signo) {
        List<Horoscopo> previsoes = horoscopoRepository.findBySigno(signo.toLowerCase());
        if (previsoes.size() < 2) {
            throw new IllegalStateException("É necessário pelo menos 2 previsões para análise lógica.");
        }

        // Normaliza e tokeniza os textos
        List<String[]> textosTokenizados = previsoes.stream()
                .map(h -> limparTexto(h.getDescricao()).split("\\s+"))
                .collect(Collectors.toList());

        // Cria um mapa de frequência de palavras
        Map<String, Integer> freqGlobal = new HashMap<>();
        for (String[] tokens : textosTokenizados) {
            for (String t : tokens) {
                freqGlobal.merge(t, 1, Integer::sum);
            }
        }

        // Palavras comuns (presentes em mais de 1 texto)
        List<String> palavrasComuns = freqGlobal.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Cria um resumo simples com base nas palavras mais frequentes
        String resumo = gerarResumo(previsoes, palavrasComuns);

        // Calcula uma “pontuação de coerência” simples
        double coerencia = Math.min(1.0, (double) palavrasComuns.size() / 20.0);

        // Cria e salva a análise
        AnaliseLogica analise = AnaliseLogica.builder()
                .signo(signo.toLowerCase())
                .data(LocalDate.now())
                .summary(resumo)
                .coherenceScore(coerencia)
                .build();

        analiseRepository.save(analise);
        return analise;
    }

    private String limparTexto(String texto) {
        return texto.toLowerCase()
                .replaceAll("[^a-záéíóúãõâêîôûç\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String gerarResumo(List<Horoscopo> previsoes, List<String> palavrasChave) {
        String base = previsoes.stream()
                .map(Horoscopo::getDescricao)
                .collect(Collectors.joining(" "));

        String[] frases = base.split("\\. ");
        List<String> frasesSelecionadas = Arrays.stream(frases)
                .filter(f -> palavrasChave.stream().anyMatch(f::contains))
                .limit(3)
                .collect(Collectors.toList());

        return frasesSelecionadas.isEmpty()
                ? "As previsões diferem em ênfases, mas indicam tendências gerais de reflexão e crescimento."
                : String.join(". ", frasesSelecionadas) + ".";
    }
}
