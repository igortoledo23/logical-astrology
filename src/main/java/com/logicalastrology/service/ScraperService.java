package com.logicalastrology.service;

import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.repository.HoroscopoRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScraperService {

    private final HoroscopoRepository horoscopoRepository;

    public ScraperService(HoroscopoRepository horoscopoRepository) {
        this.horoscopoRepository = horoscopoRepository;
    }

    /**
     * Roda automaticamente 1x por dia (pode também ser executado manualmente via endpoint).
     */
    //@Scheduled(cron = "0 0 8 * * *") // todo dia às 08:00
    public void executarScraping() {
        String signo = "aries";
        List<Horoscopo> resultados = new ArrayList<>();

        // ===============================
        // 1️⃣ Site: João Bidu
        // ===============================
        try {
            Document doc = Jsoup.connect("https://joaobidu.com.br/horoscopo-do-dia/horoscopo-do-dia-para-aries/")
                    .userAgent("Mozilla/5.0")
                    .get();

            // Normalmente, o texto principal do horóscopo está dentro de <p> dentro de uma div com classe específica
            Elements paragrafos = doc.select(".MsoNormal");
            String textoBidu = paragrafos.text();

            Horoscopo h1 = Horoscopo.builder()
                    .signo(signo)
                    .descricao(textoBidu)
                    .fonte("João Bidu")
                    .dataColeta(LocalDateTime.now())
                    .build();

            resultados.add(h1);
        } catch (Exception e) {
            System.err.println("Falha ao coletar do João Bidu: " + e.getMessage());
        }

        // ===============================
        // 2️⃣ Site: Horoscopo Virtual
        // ===============================
        try {
            Document doc = Jsoup.connect("https://www.horoscopovirtual.com.br/horoscopo/aries")
                    .userAgent("Mozilla/5.0")
                    .get();

            Elements paragrafos = doc.select(".text-wrapper");
            String textoHoroscopoVirtual = paragrafos.text();

            Horoscopo h2 = Horoscopo.builder()
                    .signo(signo)
                    .descricao(textoHoroscopoVirtual)
                    .fonte("Horoscopo Virtual")
                    .dataColeta(LocalDateTime.now())
                    .build();

            resultados.add(h2);
        } catch (Exception e) {
            System.err.println("Falha ao coletar do Horoscopo Virtual: " + e.getMessage());
        }

        // ===============================
        // 3️⃣ Persistir tudo no H2
        // ===============================
        if (!resultados.isEmpty()) {
            horoscopoRepository.saveAll(resultados);
            System.out.println("✅ " + resultados.size() + " horóscopos salvos no H2 para " + signo);
        } else {
            System.out.println("⚠️ Nenhum horóscopo coletado.");
        }

    }
}
