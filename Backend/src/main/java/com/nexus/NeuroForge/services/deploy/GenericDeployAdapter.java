// Backend/src/main/java/com/nexus/NeuroForge/services/deploy/GenericDeployAdapter.java
package com.nexus.NeuroForge.services.deploy;

import com.nexus.NeuroForge.dto.NormalizedDeployEvent;
import org.springframework.stereotype.Component;
import java.util.Map;

// A documented minimal contract anyone can POST to, regardless of platform —
// { "status": "SUCCESS", "commitHash": "...", "deployId": "..." }
@Component
public class GenericDeployAdapter implements DeployWebhookAdapter {
    @Override public String providerKey() { return "generic"; }

    @Override
    public NormalizedDeployEvent normalize(Map<String, Object> raw) {
        NormalizedDeployEvent event = new NormalizedDeployEvent();
        event.provider = "generic";
        event.status = String.valueOf(raw.getOrDefault("status", "UNKNOWN"));
        event.commitHash = (String) raw.get("commitHash");
        event.deployId = (String) raw.get("deployId");
        return event;
    }
}