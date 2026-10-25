package com.spamguard.repo;

import com.spamguard.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    List<AppUser> findByMonitoringTrue();
}
