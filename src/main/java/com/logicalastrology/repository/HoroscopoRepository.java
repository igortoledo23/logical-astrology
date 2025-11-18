package com.logicalastrology.repository;

import com.logicalastrology.model.Horoscopo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HoroscopoRepository extends JpaRepository<Horoscopo, Long> {
    List<Horoscopo> findBySignoAndDataColeta(String sign, LocalDate date);
    List<Horoscopo> findBySigno(String sign);
}
