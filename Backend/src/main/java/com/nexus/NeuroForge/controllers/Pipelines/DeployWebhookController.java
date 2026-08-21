// Backend/src/main/java/com/nexus/NeuroForge/controllers/DeployWebhookController.java
package com.nexus.NeuroForge.controllers.Pipelines;

import com.nexus.NeuroForge.dto.NormalizedDeployEvent;
import com.nexus.NeuroForge.services.deploy.DeployWebhookAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deploy-webhooks")
public class DeployWebhookController {

    @Autowired private List<DeployWebhookAdapter> adapters;

    // e.g. POST /api/deploy-webhooks/render, POST /api/deploy-webhooks/generic
    @PostMapping("/{provider}")
    public ResponseEntity<String> receive(@PathVariable String provider, @RequestBody Map<String, Object> payload) {
        DeployWebhookAdapter adapter = adapters.stream()
                .filter(a -> a.providerKey().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);

        if (adapter == null) {
            return ResponseEntity.badRequest().body("Unknown provider: " + provider);
        }

        NormalizedDeployEvent event = adapter.normalize(payload);
        System.out.println("Normalized deploy event: provider=" + event.provider
                + " status=" + event.status + " deployId=" + event.deployId);
        // TODO: persist event, e.g. as a lightweight DeployEvent row keyed by projectId
        return ResponseEntity.ok("received");
    }
}