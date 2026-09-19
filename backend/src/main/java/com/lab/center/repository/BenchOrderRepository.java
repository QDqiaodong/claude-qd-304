package com.lab.center.repository;

import com.lab.center.entity.BenchOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchOrderRepository extends JpaRepository<BenchOrder, Long> {

    Optional<BenchOrder> findByOrderNo(String orderNo);

    boolean existsByInstrumentIdAndOrderStatus(Long instrumentId, String orderStatus);

    List<BenchOrder> findAllByOrderByIdDesc();
}
