package com.logicalastrology.repository;

import com.logicalastrology.model.SignoAnalise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SignoAnaliseRepository extends JpaRepository<SignoAnalise, Long> {
    Optional<SignoAnalise> findTopBySignoIgnoreCaseAndDataAnaliseOrderByCriadoEmDesc(String signo,
                                                                                    LocalDate dataAnalise);

    List<SignoAnalise> findByDataAnalise(LocalDate dataAnalise);
}
