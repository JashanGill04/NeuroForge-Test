// Backend/src/main/java/com/nexus/NeuroForge/controllers/DeployWebhookController.java
package com.nexus.NeuroForge.controllers.Pipelines;

import com.nexus.NeuroForge.dto.CreateReleaseRequest;
import com.nexus.NeuroForge.dto.NormalizedDeployEvent;
import com.nexus.NeuroForge.models.Deployment;
import com.nexus.NeuroForge.models.Project;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.repositories.DeploymentRepository;
import com.nexus.NeuroForge.repositories.ProjectRepository;
import com.nexus.NeuroForge.services.ReleaseService;
import com.nexus.NeuroForge.services.deploy.DeployWebhookAdapter;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deploy-webhooks")
public class DeployWebhookController {

    @Autowired private List<DeployWebhookAdapter> adapters;
    @Autowired private DeploymentRepository deploymentRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ReleaseService releaseService;

    // e.g. POST /api/deploy-webhooks/render?projectId=1&environment=PRODUCTION
    // POST /api/deploy-webhooks/generic?projectId=1&environment=STAGING
    //
    // projectId and environment are query params rather than baked into the
    // adapter/payload because most hosting providers' deploy-finished webhooks
    // don't know anything about NeuroForge's project/environment model — the
    // callback URL itself is what carries that context. Copy this exact URl
    // (with your own projectId/environment) into your host's "webhook" or
    // "notification URL" setting.
    @PostMapping("/{provider}")
    public ResponseEntity<?> receive(
            @PathVariable String provider,
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "PRODUCTION") String environment,
            @RequestBody Map<String, Object> payload) {

        DeployWebhookAdapter adapter = adapters.stream()
                .filter(a -> a.providerKey().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);

        if (adapter == null) {
            return ResponseEntity.badRequest().body("Unknown provider: " + provider);
        }

        Project project;
        try {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("No project found with id " + projectId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }

        DeploymentEnvironment env;
        try {
            env = DeploymentEnvironment.valueOf(environment.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Unknown environment: " + environment
                    + " — expected one of " + java.util.Arrays.toString(DeploymentEnvironment.values()));
        }

        NormalizedDeployEvent event = adapter.normalize(payload);
        boolean success = "SUCCESS".equalsIgnoreCase(event.status) || "LIVE".equalsIgnoreCase(event.status)
                || "DEPLOYED".equalsIgnoreCase(event.status);

        // Persist the deployment. No Pipeline is attached here — this deployment
        // came straight from the host, not from a NeuroForge-triggered CI run —
        // so `project` is set directly (see Deployment.resolveProjectId()).
        Deployment deployment = new Deployment();
        deployment.setProject(project);
        deployment.setEnvironment(env);
        deployment.setSuccess(success);
        deployment.setDeployedAt(LocalDateTime.now());
        deployment.setImageTag(event.commitHash != null ? event.commitHash : event.deployId);
        // No prior-success lookup needed for rollback eligibility here — rollback
        // for webhook-originated deployments isn't supported yet (there's no CI
        // pipeline to re-dispatch); see ReleaseService.rollbackRelease.
        deployment.setRollbackEligible(false);
        deployment = deploymentRepository.save(deployment);

        System.out.println("Recorded deploy event: provider=" + event.provider
                + " status=" + event.status + " deployId=" + event.deployId
                + " project=" + projectId + " environment=" + env);

        Long releaseId = null;
        if (success) {
            try {
                CreateReleaseRequest req = new CreateReleaseRequest();
                req.setDeploymentId(deployment.getId());
                req.setApproved(true);
                releaseId = releaseService.createRelease(req).getId();
            } catch (Exception e) {
                // Don't fail the webhook response over release bookkeeping — the
                // deployment itself is already safely recorded either way.
                System.err.println("Deployment recorded but auto-release failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "recorded",
                "deploymentId", deployment.getId(),
                "releaseId", releaseId != null ? releaseId : "none"
        ));
    }
}