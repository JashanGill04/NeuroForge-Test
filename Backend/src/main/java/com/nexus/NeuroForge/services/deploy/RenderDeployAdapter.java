// Backend/src/main/java/com/nexus/NeuroForge/services/deploy/RenderDeployAdapter.java
package com.nexus.NeuroForge.services.deploy;

import com.nexus.NeuroForge.dto.NormalizedDeployEvent;
import org.springframework.stereotype.Component;
import java.util.Map;

// NOTE: adjust field paths once you've logged one real Render payload —
// their notification shape isn't fully documented, this is a starting guess.
@Component
public class RenderDeployAdapter implements DeployWebhookAdapter {
    @Override public String providerKey() { return "render"; }

    @Override
    public NormalizedDeployEvent normalize(Map<String, Object> raw) {
        NormalizedDeployEvent event = new NormalizedDeployEvent();
        event.provider = "render";
        event.status = String.valueOf(raw.getOrDefault("status", "UNKNOWN"));
        event.deployId = String.valueOf(raw.getOrDefault("id", ""));
        return event;
    }
}