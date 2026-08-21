// Backend/src/main/java/com/nexus/NeuroForge/repositories/MonitoringTargetRepository.java
package com.nexus.NeuroForge.repositories;

import com.nexus.NeuroForge.models.MonitoringTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonitoringTargetRepository extends JpaRepository<MonitoringTarget, Long> {
    List<MonitoringTarget> findByProject_IdAndEnabledTrue(Long projectId);
    List<MonitoringTarget> findByEnabledTrue();
    List<MonitoringTarget> findByProject_Id(Long projectId);
}