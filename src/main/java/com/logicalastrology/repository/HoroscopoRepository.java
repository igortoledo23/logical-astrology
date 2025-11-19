package com.logicalastrology.repository;

import com.logicalastrology.model.Horoscopo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HoroscopoRepository extends JpaRepository<Horoscopo, Long> {

    List<Horoscopo> findBySignoIgnoreCase(String sign);

    List<Horoscopo> findBySignoIgnoreCaseAndDataColetaBetween(String sign,
                                                             LocalDateTime start,
                                                             LocalDateTime end);

    List<Horoscopo> findTop10BySignoIgnoreCaseOrderByDataColetaDesc(String sign);

    boolean existsBySignoIgnoreCaseAndFonteIgnoreCaseAndDataColetaBetween(String signo,
                                                                           String fonte,
                                                                           LocalDateTime inicio,
                                                                           LocalDateTime fim);
}
