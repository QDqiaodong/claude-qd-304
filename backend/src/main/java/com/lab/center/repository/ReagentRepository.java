package com.lab.center.repository;

import com.lab.center.entity.Reagent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReagentRepository extends JpaRepository<Reagent, Long> {

    Optional<Reagent> findByReagentCode(String reagentCode);

    List<Reagent> findAllByOrderByIdAsc();

    long countByCabinetId(Long cabinetId);
}
