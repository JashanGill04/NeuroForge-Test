// Backend/src/main/java/com/nexus/NeuroForge/models/MonitoringTarget.java
package com.nexus.NeuroForge.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.interfaces.ProbeStrategy;
import jakarta.persistence.*;

@Entity
@Table(name = "monitoring_targets")
public class MonitoringTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    private String label;                    // e.g. "Production API", "Staging worker"

    @Enumerated(EnumType.STRING)
    private DeploymentEnvironment environment; // reuse your existing enum

    private String baseUrl;                   // e.g. https://your-app.onrender.com
    private String healthCheckPath = "/health";

    @Enumerated(EnumType.STRING)
    private ProbeStrategy probeStrategy = ProbeStrategy.HTTP_PING; // default: works anywhere

    // Only used when probeStrategy == PROMETHEUS_SCRAPE
    private String prometheusJobName;
    private String metricsToken;

    private String providerLabel; // free text: "render", "vercel", "aws", "self-hosted"... cosmetic only

    private boolean enabled = true;

    public MonitoringTarget() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getHealthCheckPath() { return healthCheckPath; }
    public void setHealthCheckPath(String healthCheckPath) { this.healthCheckPath = healthCheckPath; }
    public ProbeStrategy getProbeStrategy() { return probeStrategy; }
    public void setProbeStrategy(ProbeStrategy probeStrategy) { this.probeStrategy = probeStrategy; }
    public String getPrometheusJobName() { return prometheusJobName; }
    public void setPrometheusJobName(String prometheusJobName) { this.prometheusJobName = prometheusJobName; }
    public String getMetricsToken() { return metricsToken; }
    public void setMetricsToken(String metricsToken) { this.metricsToken = metricsToken; }
    public String getProviderLabel() { return providerLabel; }
    public void setProviderLabel(String providerLabel) { this.providerLabel = providerLabel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}