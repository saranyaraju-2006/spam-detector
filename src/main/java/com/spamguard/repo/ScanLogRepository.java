package com.spamguard.repo;

import com.spamguard.model.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {
    List<ScanLog> findTop100ByUserIdOrderByScannedAtDesc(Long userId);
    List<ScanLog> findTop200ByOrderByScannedAtDesc();
    List<ScanLog> findTop200ByLoginIdOrderByScannedAtDesc(String loginId);
    long countByUserId(Long userId);
    long countByUserIdAndLabel(Long userId, String label);
}
