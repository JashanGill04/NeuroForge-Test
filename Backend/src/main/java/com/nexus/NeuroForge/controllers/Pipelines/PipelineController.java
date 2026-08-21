package com.nexus.NeuroForge.controllers.Pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.NeuroForge.dto.*;
import com.nexus.NeuroForge.models.Pipeline;
import com.nexus.NeuroForge.services.PipelineService;
import com.nexus.NeuroForge.services.ProjectIntegrationService;
import com.nexus.NeuroForge.services.WebhookSignatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    @Autowired private PipelineService pipelineService;
    @Autowired private ProjectIntegrationService projectIntegrationService;
    @Autowired private WebhookSignatureValidator webhookSignatureValidator;
    @Autowired private ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveBuildResult(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        PipelineWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PipelineWebhookRequest.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Malformed webhook payload");
        }

        if (request.getProjectId() == null) {
            return ResponseEntity.badRequest().body("projectId is required");
        }

        String webhookSecret;
        try {
            webhookSecret = projectIntegrationService.getEntityOrThrow(request.getProjectId()).getWebhookSecret();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }

        if (!webhookSignatureValidator.isValid(rawBody, signature, webhookSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        }

        Pipeline saved = pipelineService.recordBuildResult(request);
        return ResponseEntity.ok(Map.of(
                "status", "recorded",
                "pipelineId", saved.getId()
        ));
    }
    @GetMapping
    public List<PipelineResponse> getHistory(@RequestParam Long projectId) {
        return pipelineService.getHistory(projectId);
    }

    @GetMapping("/kpi")
    public PipelineKpiDTO getKpis(@RequestParam Long projectId) {
        return pipelineService.getKpis(projectId);
    }

    @GetMapping("/{id}")
    public PipelineDetailDTO getDetail(@PathVariable Long id) {
        return pipelineService.getDetail(id);
    }

    @PostMapping("/trigger/{projectId}")
    public ResponseEntity<String> triggerPipeline(@PathVariable Long projectId) {
        pipelineService.triggerJenkinsBuild(projectId);
        return ResponseEntity.ok("Pipeline triggered successfully");
    }

    @PostMapping("/{pipelineId}/rollback")
    public ResponseEntity<String> rollbackDeployment(@PathVariable Long pipelineId) {
        pipelineService.executeRollback(pipelineId);
        return ResponseEntity.ok("Rollback initiated");
    }
}