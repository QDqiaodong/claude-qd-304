package com.lab.center.repository;

import com.lab.center.entity.Cabinet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    Optional<Cabinet> findByCabinetCode(String cabinetCode);

    List<Cabinet> findAllByOrderByIdAsc();
}
