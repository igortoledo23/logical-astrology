package com.logicalastrology.repository;

import com.logicalastrology.model.AnaliseLogica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AnaliseLogicaRepository extends JpaRepository<AnaliseLogica, Long> {
    Optional<AnaliseLogica> findBySignoAndData(String sign, LocalDate date);
}
