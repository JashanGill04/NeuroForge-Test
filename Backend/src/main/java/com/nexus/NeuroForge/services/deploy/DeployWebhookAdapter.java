// Backend/src/main/java/com/nexus/NeuroForge/services/deploy/DeployWebhookAdapter.java
package com.nexus.NeuroForge.services.deploy;

import com.nexus.NeuroForge.dto.NormalizedDeployEvent;
import java.util.Map;

public interface DeployWebhookAdapter {
    String providerKey(); // "render", "vercel", "generic", ...
    NormalizedDeployEvent normalize(Map<String, Object> rawPayload);
}