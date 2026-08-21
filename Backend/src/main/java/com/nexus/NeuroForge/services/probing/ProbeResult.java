// Backend/src/main/java/com/nexus/NeuroForge/services/probing/ProbeResult.java
package com.nexus.NeuroForge.services.probing;

public class ProbeResult {
    public final boolean up;
    public final long responseTimeMs;
    public ProbeResult(boolean up, long responseTimeMs) {
        this.up = up;
        this.responseTimeMs = responseTimeMs;
    }
}