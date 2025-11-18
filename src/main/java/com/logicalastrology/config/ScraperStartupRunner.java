package com.logicalastrology.config;

import com.logicalastrology.service.ScraperService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ScraperStartupRunner implements CommandLineRunner {

    private final ScraperService scraperService;

    public ScraperStartupRunner(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Executando scraper inicial...");
        scraperService.executarScraping();
    }
}
