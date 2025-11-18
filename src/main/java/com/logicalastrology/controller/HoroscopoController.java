package com.logicalastrology.controller;

import com.logicalastrology.dto.AnaliseDTO;
import com.logicalastrology.dto.HoroscopoDTO;
import com.logicalastrology.service.AnaliseService;
import com.logicalastrology.service.HoroscopoService;
import com.logicalastrology.model.AnaliseLogica;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HoroscopoController {

    private final HoroscopoService horoscopoService;
    private final AnaliseService analiseService;

    public HoroscopoController(HoroscopoService horoscopoService, AnaliseService analiseService) {
        this.horoscopoService = horoscopoService;
        this.analiseService = analiseService;
    }

    @GetMapping("/horoscopos/{sign}")
    public ResponseEntity<List<HoroscopoDTO>> getHoroscopos(@PathVariable("sign") String sign) {
        return ResponseEntity.ok(horoscopoService.findBySign(sign));
    }


}
