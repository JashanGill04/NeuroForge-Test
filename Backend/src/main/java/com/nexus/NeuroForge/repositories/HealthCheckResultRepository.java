// Backend/src/main/java/com/nexus/NeuroForge/repositories/HealthCheckResultRepository.java
package com.nexus.NeuroForge.repositories;

import com.nexus.NeuroForge.models.HealthCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface HealthCheckResultRepository extends JpaRepository<HealthCheckResult, Long> {
    long countByProjectIdAndCheckedAtAfter(Long projectId, LocalDateTime after);
    long countByProjectIdAndUpTrueAndCheckedAtAfter(Long projectId, LocalDateTime after);
    List<HealthCheckResult> findByProjectIdAndCheckedAtAfterOrderByCheckedAtAsc(Long projectId, LocalDateTime after);
    HealthCheckResult findTopByProjectIdOrderByCheckedAtDesc(Long projectId);
    HealthCheckResult findTopByTargetIdOrderByCheckedAtDesc(Long targetId);
}