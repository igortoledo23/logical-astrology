package com.logicalastrology.controller;

import com.logicalastrology.dto.AnaliseComparativaDTO;
import com.logicalastrology.dto.AnaliseSignoDTO;
import com.logicalastrology.dto.DataRequest;
import com.logicalastrology.dto.HoroscopoDTO;
import com.logicalastrology.service.AnaliseSignoService;
import com.logicalastrology.service.HoroscopoService;
import com.logicalastrology.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HoroscopoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HoroscopoController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HoroscopoService horoscopoService;
    private final AnaliseSignoService analiseSignoService;
    private final ScraperService scraperService;

    public HoroscopoController(HoroscopoService horoscopoService,
                               AnaliseSignoService analiseSignoService,
                               ScraperService scraperService) {
        this.horoscopoService = horoscopoService;
        this.analiseSignoService = analiseSignoService;
        this.scraperService = scraperService;
    }

    @GetMapping("/horoscopos/{sign}")
    public ResponseEntity<List<HoroscopoDTO>> getHoroscopos(@PathVariable("sign") String sign,
                                                            @RequestBody(required = false) DataRequest dataRequest) {
        LocalDate data = resolveDate(dataRequest);
        LOGGER.info("Endpoint /horoscopos/{} chamado para a data {}", sign, data);
        return ResponseEntity.ok(horoscopoService.findBySign(sign, data));
    }

    @GetMapping("/horoscopos")
    public ResponseEntity<List<HoroscopoDTO>> getHoroscoposPorData(@RequestBody(required = false) DataRequest dataRequest) {
        LocalDate data = resolveDate(dataRequest);
        LOGGER.info("Endpoint /horoscopos chamado para a data {}", data);
        return ResponseEntity.ok(horoscopoService.findAllByDate(data));
    }

    @GetMapping("/analise/{sign}")
    public ResponseEntity<AnaliseSignoDTO> analisar(@PathVariable("sign") String sign,
                                                    @RequestBody(required = false) DataRequest dataRequest) {
        LocalDate data = resolveDate(dataRequest);
        LOGGER.info("Endpoint /analise/{} chamado para a data {}", sign, data);
        return ResponseEntity.ok(analiseSignoService.analisar(sign, data));
    }

    @GetMapping("/analise/todos")
    public ResponseEntity<List<AnaliseSignoDTO>> analisarTodos(@RequestBody(required = false) DataRequest dataRequest) {
        LocalDate data = resolveDate(dataRequest);
        LOGGER.info("Endpoint /analise/todos chamado para a data {}", data);
        return ResponseEntity.ok(analiseSignoService.analisarTodos(data));
    }

    @GetMapping("/analise/comparativo/{sign}")
    public ResponseEntity<List<AnaliseComparativaDTO>> comparativo(@PathVariable("sign") String sign,
                                                                   @RequestBody(required = false) DataRequest dataRequest) {
        LocalDate data = resolveDate(dataRequest);
        LOGGER.info("Endpoint /analise/comparativo/{} chamado para a data {}", sign, data);
        return ResponseEntity.ok(analiseSignoService.obterComparativo(sign, data));
    }

    @PostMapping("/scrapper")
    public ResponseEntity<String> executarScrapper() {
        LOGGER.info("Endpoint /scrapper chamado para executar scraping manual");
        scraperService.executarScraping();
        return ResponseEntity.ok("Scraping executado com sucesso");
    }

    private LocalDate resolveDate(DataRequest dataRequest) {
        if (dataRequest == null || dataRequest.getData() == null || dataRequest.getData().isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dataRequest.getData(), FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data em formato inválido. Use dd/MM/yyyy.");
        }
    }
}
