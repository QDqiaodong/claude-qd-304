package com.lab.center.repository;

import com.lab.center.entity.Instrument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByInstrumentCode(String instrumentCode);

    List<Instrument> findAllByOrderByIdAsc();
}
