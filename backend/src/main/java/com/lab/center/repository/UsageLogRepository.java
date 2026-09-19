package com.lab.center.repository;

import com.lab.center.entity.UsageLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findByLogNo(String logNo);

    List<UsageLog> findAllByOrderByIdDesc();

    List<UsageLog> findByReagentIdOrderByIdDesc(Long reagentId);
}
