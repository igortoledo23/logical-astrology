package com.logicalastrology.controller;

import com.logicalastrology.model.HoroscopoConsolidado;
import com.logicalastrology.service.AnaliseLogicaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analise")
public class AnaliseLogicaController {

    private final AnaliseLogicaService analiseService;

    public AnaliseLogicaController(AnaliseLogicaService analiseService) {
        this.analiseService = analiseService;
    }

    @PostMapping("/{signo}")
    public HoroscopoConsolidado analisar(@PathVariable("signo") String signo) {
        return analiseService.analisarSigno(signo);
    }
}
