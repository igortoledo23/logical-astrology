package com.logicalastrology.service;

import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.repository.HoroscopoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ScraperService {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String JOAO_BIDU_TEMPLATE =
            "https://joaobidu.com.br/horoscopo-do-dia/horoscopo-do-dia-para-%s/";
    private static final String HOROSCOPO_VIRTUAL_TEMPLATE =
            "https://www.horoscopovirtual.com.br/horoscopo/%s";
    private static final String PERSONARE_TEMPLATE =
            "https://www.personare.com.br/horoscopo-do-dia/%s";

    private static final List<SignoConfig> SIGNOS = List.of(
            new SignoConfig("aries"),
            new SignoConfig("touro"),
            new SignoConfig("gemeos"),
            new SignoConfig("cancer"),
            new SignoConfig("leao"),
            new SignoConfig("virgem"),
            new SignoConfig("libra"),
            new SignoConfig("escorpiao"),
            new SignoConfig("sagitario"),
            new SignoConfig("capricornio"),
            new SignoConfig("aquario"),
            new SignoConfig("peixes")
    );

    private final HoroscopoRepository horoscopoRepository;

    public ScraperService(HoroscopoRepository horoscopoRepository) {
        this.horoscopoRepository = horoscopoRepository;
    }

    /**
     * Roda automaticamente 1x por dia (pode também ser executado manualmente via endpoint).
     */
    //@Scheduled(cron = "0 0 8 * * *") // todo dia às 08:00
    public void executarScraping() {
        List<Horoscopo> resultados = new ArrayList<>();

        for (SignoConfig signo : SIGNOS) {
            resultados.addAll(coletarPorSigno(signo));
        }

        if (!resultados.isEmpty()) {
            horoscopoRepository.saveAll(resultados);
            log.info("✅ {} horóscopos salvos para {} signos", resultados.size(), SIGNOS.size());
        } else {
            log.warn("⚠️ Nenhum horóscopo coletado.");
        }
    }

    private List<Horoscopo> coletarPorSigno(SignoConfig signo) {
        List<Horoscopo> coletados = new ArrayList<>();
        scrapeJoaoBidu(signo).ifPresent(coletados::add);
        scrapeHoroscopoVirtual(signo).ifPresent(coletados::add);
        scrapePersonare(signo).ifPresent(coletados::add);
        return coletados;
    }

    private Optional<Horoscopo> scrapeJoaoBidu(SignoConfig signo) {
        String url = String.format(JOAO_BIDU_TEMPLATE, signo.joaoBiduSlug());
        try {
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
            String texto = doc.select(".MsoNormal").text();
            if (texto.isBlank()) {
                texto = doc.select("article p").text();
            }
            return buildHoroscopo(signo.nome(), "João Bidu", texto);
        } catch (Exception e) {
            log.warn("Falha ao coletar João Bidu para {}: {}", signo.nome(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Horoscopo> scrapeHoroscopoVirtual(SignoConfig signo) {
        String url = String.format(HOROSCOPO_VIRTUAL_TEMPLATE, signo.horoscopoVirtualSlug());
        try {
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
            String texto = doc.select(".text-wrapper p").text();
            if (texto.isBlank()) {
                texto = doc.select(".text-wrapper").text();
            }
            return buildHoroscopo(signo.nome(), "Horoscopo Virtual", texto);
        } catch (Exception e) {
            log.warn("Falha ao coletar Horoscopo Virtual para {}: {}", signo.nome(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Horoscopo> scrapePersonare(SignoConfig signo) {
        String url = String.format(PERSONARE_TEMPLATE, signo.personareSlug());
        try {
            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
            String texto = doc.select("div.sc-6d2a5120-5.fgTbnr p").text();
            if (texto.isBlank()) {
                texto = doc.select("div.sc-6d2a5120-5.fgTbnr").text();
            }
            return buildHoroscopo(signo.nome(), "Personare", texto);
        } catch (Exception e) {
            log.warn("Falha ao coletar Personare para {}: {}", signo.nome(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Horoscopo> buildHoroscopo(String signo, String fonte, String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Horoscopo.builder()
                .signo(signo)
                .descricao(descricao)
                .fonte(fonte)
                .dataColeta(LocalDateTime.now())
                .build());
    }

    private record SignoConfig(String nome, String joaoBiduSlug, String horoscopoVirtualSlug, String personareSlug) {
        private SignoConfig(String nome) {
            this(nome, nome, nome, nome);
        }
    }
}
