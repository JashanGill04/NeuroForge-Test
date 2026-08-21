// Backend/src/main/java/com/nexus/NeuroForge/dto/MonitoringTargetRequest.java
package com.nexus.NeuroForge.dto;

import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.interfaces.ProbeStrategy;

public class MonitoringTargetRequest {
    private String label;
    private DeploymentEnvironment environment;
    private String baseUrl;
    private String healthCheckPath;
    private ProbeStrategy probeStrategy;
    private String prometheusJobName;
    private String metricsToken;
    private String providerLabel;
    private boolean enabled = true;

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