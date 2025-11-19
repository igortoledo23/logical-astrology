package com.logicalastrology.service;

import com.logicalastrology.dto.HoroscopoDTO;
import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.repository.HoroscopoRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HoroscopoService {

    private final HoroscopoRepository repository;

    public HoroscopoService(HoroscopoRepository repository) {
        this.repository = repository;
    }

    public List<HoroscopoDTO> findBySign(String sign) {
        List<Horoscopo> list = repository.findBySignoIgnoreCase(sign);
        return list.stream().map(h -> HoroscopoDTO.builder()
                .sign(h.getSigno())
                .source(h.getFonte())
                .text(h.getDescricao())
                .build()).collect(Collectors.toList());
    }

    public void save(Horoscopo h) {
        if (h.getDataColeta() == null) h.setDataColeta(LocalDateTime.now());
        repository.save(h);
    }
}
