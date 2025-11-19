package com.logicalastrology.controller;

import com.logicalastrology.dto.AnaliseSignoDTO;
import com.logicalastrology.dto.HoroscopoDTO;
import com.logicalastrology.service.AnaliseSignoService;
import com.logicalastrology.service.HoroscopoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HoroscopoController {

    private final HoroscopoService horoscopoService;
    private final AnaliseSignoService analiseSignoService;

    public HoroscopoController(HoroscopoService horoscopoService,
                               AnaliseSignoService analiseSignoService) {
        this.horoscopoService = horoscopoService;
        this.analiseSignoService = analiseSignoService;
    }

    @GetMapping("/horoscopos/{sign}")
    public ResponseEntity<List<HoroscopoDTO>> getHoroscopos(@PathVariable("sign") String sign) {
        return ResponseEntity.ok(horoscopoService.findBySign(sign));
    }

    @GetMapping("/analise/{sign}")
    public ResponseEntity<AnaliseSignoDTO> analisar(@PathVariable("sign") String sign) {
        return ResponseEntity.ok(analiseSignoService.analisar(sign));
    }
}
