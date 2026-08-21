// Backend/src/main/java/com/nexus/NeuroForge/services/MonitoringTargetService.java
package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.MonitoringTargetRequest;
import com.nexus.NeuroForge.dto.MonitoringTargetResponse;
import com.nexus.NeuroForge.models.HealthCheckResult;
import com.nexus.NeuroForge.models.MonitoringTarget;
import com.nexus.NeuroForge.models.Project;
import com.nexus.NeuroForge.repositories.HealthCheckResultRepository;
import com.nexus.NeuroForge.repositories.MonitoringTargetRepository;
import com.nexus.NeuroForge.repositories.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitoringTargetService {

    @Autowired private MonitoringTargetRepository monitoringTargetRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private HealthCheckResultRepository healthCheckResultRepository;

    public MonitoringTargetResponse create(Long projectId, MonitoringTargetRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        MonitoringTarget target = new MonitoringTarget();
        target.setProject(project);
        apply(target, req);
        return toResponse(monitoringTargetRepository.save(target));
    }

    // Backend/src/main/java/com/nexus/NeuroForge/services/MonitoringTargetService.java
    public List<MonitoringTargetResponse> getByProject(Long projectId) {
        return monitoringTargetRepository.findByProject_Id(projectId).stream()
                .map(this::toResponse).toList();
    }

    // MonitoringTargetService.java — add
    public MonitoringTargetResponse update(Long id, MonitoringTargetRequest req) {
        MonitoringTarget target = monitoringTargetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Monitoring target not found: " + id));
        apply(target, req);
        return toResponse(monitoringTargetRepository.save(target));
    }

    public void delete(Long id) {
        monitoringTargetRepository.deleteById(id);
    }

    private void apply(MonitoringTarget target, MonitoringTargetRequest req) {
        target.setLabel(req.getLabel());
        target.setEnvironment(req.getEnvironment());
        target.setBaseUrl(req.getBaseUrl());
        target.setHealthCheckPath(req.getHealthCheckPath() != null ? req.getHealthCheckPath() : "/health");
        target.setProbeStrategy(req.getProbeStrategy());
        target.setPrometheusJobName(req.getPrometheusJobName());
        target.setProviderLabel(req.getProviderLabel());
        target.setEnabled(req.isEnabled());
        if (req.getMetricsToken() != null && !req.getMetricsToken().isBlank()) {
            target.setMetricsToken(req.getMetricsToken());
        }
    }

    private MonitoringTargetResponse toResponse(MonitoringTarget t) {
        MonitoringTargetResponse r = new MonitoringTargetResponse();
        r.id = t.getId();
        r.label = t.getLabel();
        r.environment = t.getEnvironment() != null ? t.getEnvironment().name() : null;
        r.baseUrl = t.getBaseUrl();
        r.healthCheckPath = t.getHealthCheckPath();
        r.probeStrategy = t.getProbeStrategy() != null ? t.getProbeStrategy().name() : null;
        r.prometheusJobName = t.getPrometheusJobName();
        r.providerLabel = t.getProviderLabel();
        r.enabled = t.isEnabled();

        // Backend/src/main/java/com/nexus/NeuroForge/services/MonitoringTargetService.java
// inside toResponse(MonitoringTarget t), replace the lookup:
        HealthCheckResult latest = healthCheckResultRepository.findTopByTargetIdOrderByCheckedAtDesc(t.getId());
        if (latest != null) {
            r.lastUp = latest.isUp();
            r.lastCheckedAt = latest.getCheckedAt().toString();
            r.lastResponseTimeMs = latest.getResponseTimeMs();
        }
        return r;
    }
}