package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.ProjectIntegrationRequest;
import com.nexus.NeuroForge.dto.ProjectIntegrationResponse;
import com.nexus.NeuroForge.models.Project;
import com.nexus.NeuroForge.models.ProjectIntegration;
import com.nexus.NeuroForge.repositories.ProjectIntegrationRepository;
import com.nexus.NeuroForge.repositories.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ProjectIntegrationService {

    @Autowired private ProjectIntegrationRepository integrationRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TokenEncryptionService tokenEncryptionService;

    public ProjectIntegrationResponse getByProject(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        return toResponse(integration);
    }

    public ProjectIntegrationResponse connect(Long projectId, ProjectIntegrationRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseGet(() -> {
                    ProjectIntegration fresh = new ProjectIntegration();
                    fresh.setProject(project);
                    fresh.setWebhookSecret(generateWebhookSecret());
                    return fresh;
                });

        integration.setGithubOwner(req.getGithubOwner());
        integration.setGithubRepo(req.getGithubRepo());
        integration.setGithubBranch(req.getGithubBranch() != null ? req.getGithubBranch() : "main");
        integration.setWorkflowFile(req.getWorkflowFile() != null ? req.getWorkflowFile() : "ci-cd.yml");

        // NEW — deploy hook URL. Blank/null just means "no real deploy step",
        // same as leaving the GitHub token blank means "keep the existing one".
        integration.setDeployHookUrl(req.getDeployHookUrl());

        // Only overwrite the stored token if the user actually typed a new one —
        // leaving the field blank on an edit keeps the existing connected token.
        if (req.getGithubToken() != null && !req.getGithubToken().isBlank()) {
            integration.setGithubTokenEncrypted(tokenEncryptionService.encrypt(req.getGithubToken()));
        }

        return toResponse(integrationRepository.save(integration));
    }

    public ProjectIntegrationResponse regenerateWebhookSecret(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        integration.setWebhookSecret(generateWebhookSecret());
        return toResponse(integrationRepository.save(integration));
    }

    public void disconnect(Long projectId) {
        ProjectIntegration integration = integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new EntityNotFoundException("No GitHub integration connected for project " + projectId));
        integrationRepository.delete(integration);
    }

    // Used internally by PipelineService — never exposed over the API with
    // a decrypted token attached.
    public ProjectIntegration getEntityOrThrow(Long projectId) {
        return integrationRepository.findByProject_Id(projectId)
                .orElseThrow(() -> new IllegalStateException(
                        "Project " + projectId + " has no GitHub repository connected. Connect one from Project Settings."));
    }

    public String decryptToken(ProjectIntegration integration) {
        return tokenEncryptionService.decrypt(integration.getGithubTokenEncrypted());
    }

    private String generateWebhookSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ProjectIntegrationResponse toResponse(ProjectIntegration i) {
        return new ProjectIntegrationResponse(
                i.getId(),
                i.getProject().getId(),
                i.getGithubOwner(),
                i.getGithubRepo(),
                i.getGithubBranch(),
                i.getWorkflowFile(),
                i.getWebhookSecret(),
                i.getGithubTokenEncrypted() != null,
                i.getDeployHookUrl()
        );
    }
}