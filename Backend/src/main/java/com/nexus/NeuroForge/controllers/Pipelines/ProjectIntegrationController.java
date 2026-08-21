package com.nexus.NeuroForge.controllers.Pipelines;

import com.nexus.NeuroForge.dto.ProjectIntegrationRequest;
import com.nexus.NeuroForge.dto.ProjectIntegrationResponse;
import com.nexus.NeuroForge.services.ProjectIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/integration")
public class ProjectIntegrationController {

    @Autowired private ProjectIntegrationService integrationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectIntegrationResponse> get(@PathVariable Long projectId) {
        return ResponseEntity.ok(integrationService.getByProject(projectId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectIntegrationResponse> connect(
            @PathVariable Long projectId, @RequestBody ProjectIntegrationRequest req) {
        return ResponseEntity.ok(integrationService.connect(projectId, req));
    }

    @PostMapping("/regenerate-secret")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ProjectIntegrationResponse> regenerateSecret(@PathVariable Long projectId) {
        return ResponseEntity.ok(integrationService.regenerateWebhookSecret(projectId));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disconnect(@PathVariable Long projectId) {
        integrationService.disconnect(projectId);
        return ResponseEntity.noContent().build();
    }
}