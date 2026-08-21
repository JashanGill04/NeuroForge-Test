// Backend/src/main/java/com/nexus/NeuroForge/services/ExternalHealthMonitorService.java
package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.models.HealthCheckResult;
import com.nexus.NeuroForge.models.MonitoringTarget;
import com.nexus.NeuroForge.repositories.HealthCheckResultRepository;
import com.nexus.NeuroForge.repositories.MonitoringTargetRepository;
import com.nexus.NeuroForge.services.probing.Prober;
import com.nexus.NeuroForge.services.probing.ProbeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExternalHealthMonitorService {

    @Autowired private MonitoringTargetRepository monitoringTargetRepository;
    @Autowired private HealthCheckResultRepository healthCheckResultRepository;

    // Spring injects every @Component implementing Prober into this list —
    // adding a new probe strategy later (e.g. gRPC health check, TCP ping)
    // means writing one new class, no changes here.
    @Autowired private List<Prober> probers;

    private Map<com.nexus.NeuroForge.models.interfaces.ProbeStrategy, Prober> proberByStrategy;

    @jakarta.annotation.PostConstruct
    void init() {
        proberByStrategy = probers.stream().collect(Collectors.toMap(Prober::supports, p -> p));
    }

    @Scheduled(fixedRate = 30000)
    public void pollAllTargets() {
        for (MonitoringTarget target : monitoringTargetRepository.findByEnabledTrue()) {
            Prober prober = proberByStrategy.get(target.getProbeStrategy());
            if (prober == null) continue; // unknown/unimplemented strategy — skip safely

            ProbeResult result = prober.probe(target);

            HealthCheckResult saved = new HealthCheckResult();
            saved.setProjectId(target.getProject().getId());
            saved.setTargetId(target.getId());
            saved.setUp(result.up);
            saved.setResponseTimeMs(result.responseTimeMs);
            saved.setCheckedAt(LocalDateTime.now());
            healthCheckResultRepository.save(saved);
        }
    }
}