// Backend/src/main/java/com/nexus/NeuroForge/services/probing/Prober.java
package com.nexus.NeuroForge.services.probing;

import com.nexus.NeuroForge.models.MonitoringTarget;
import com.nexus.NeuroForge.models.interfaces.ProbeStrategy;

public interface Prober {
    ProbeStrategy supports();
    ProbeResult probe(MonitoringTarget target);
}