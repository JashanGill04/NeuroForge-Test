// Backend/src/main/java/com/nexus/NeuroForge/dto/MonitoringTargetResponse.java
package com.nexus.NeuroForge.dto;

public class MonitoringTargetResponse {
    public Long id;
    public String label;
    public String environment;
    public String baseUrl;
    public String healthCheckPath;
    public String probeStrategy;
    public String prometheusJobName;
    public String providerLabel;
    public boolean enabled;
    // "latest" fields so a quick GET tells you if it's currently healthy without a second query
    public Boolean lastUp;
    public String lastCheckedAt;
    // Backend/src/main/java/com/nexus/NeuroForge/dto/MonitoringTargetResponse.java
    public Long lastResponseTimeMs;
}
