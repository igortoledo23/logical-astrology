package com.logicalastrology.service;

import com.logicalastrology.dto.HoroscopoDTO;
import com.logicalastrology.model.Horoscopo;
import com.logicalastrology.repository.HoroscopoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HoroscopoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HoroscopoService.class);

    private final HoroscopoRepository repository;

    public HoroscopoService(HoroscopoRepository repository) {
        this.repository = repository;
    }

    public List<HoroscopoDTO> findBySign(String sign, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LOGGER.info("Buscando horóscopos para o signo {} na data {}", sign, targetDate);
        List<Horoscopo> list = repository.findBySignoIgnoreCaseAndDataColetaBetween(sign,
                targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<HoroscopoDTO> findAllByDate(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LOGGER.info("Buscando todos os horóscopos na data {}", targetDate);
        List<Horoscopo> list = repository.findByDataColetaBetween(targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay());
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Horoscopo> findEntitiesBySignAndDate(String sign, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return repository.findBySignoIgnoreCaseAndDataColetaBetween(sign,
                targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
    }

    public void save(Horoscopo h) {
        if (h.getDataColeta() == null) h.setDataColeta(LocalDateTime.now());
        repository.save(h);
    }

    private HoroscopoDTO toDto(Horoscopo h) {
        return HoroscopoDTO.builder()
                .sign(h.getSigno())
                .source(h.getFonte())
                .text(h.getDescricao())
                .build();
    }
}
