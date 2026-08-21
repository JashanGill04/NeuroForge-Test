// Backend/src/main/java/com/nexus/NeuroForge/models/interfaces/ProbeStrategy.java
package com.nexus.NeuroForge.models.interfaces;

public enum ProbeStrategy {
    HTTP_PING,        // works with any deployment platform, zero setup on their side
    PROMETHEUS_SCRAPE // requires /metrics + a scrape job in prometheus.yml
}