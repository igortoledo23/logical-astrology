package com.logicalastrology.scheduler;

import com.logicalastrology.scraper.BaseScraper;
import com.logicalastrology.service.HoroscopoService;
import com.logicalastrology.model.Horoscopo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ScraperScheduler {

    private final BaseScraper scraper;
    private final HoroscopoService service;

    public ScraperScheduler(BaseScraper scraper, HoroscopoService service) {
        this.scraper = scraper;
        this.service = service;
    }

    // runs every day at 06:00
    @Scheduled(cron = "0 0 6 * * *")
    public void dailyJob() {
        try {
            Map<String, String> results = scraper.fetchAll();
            // For demo store a single sample record
            String sample = results.getOrDefault("sample", "");
            Horoscopo h = Horoscopo.builder()
                    .signo("sample")
                    .fonte("personare")
                    .text(sample)
                    .dataColeta(LocalDateTime.now())
                    .build();
            service.save(h);
        } catch (Exception e) {
            // in production use logger
            e.printStackTrace();
        }
    }
}
