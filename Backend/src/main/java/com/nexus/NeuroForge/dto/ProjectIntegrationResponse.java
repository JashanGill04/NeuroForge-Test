package com.nexus.NeuroForge.dto;

public class ProjectIntegrationResponse {
    private Long id;
    private Long projectId;
    private String githubOwner;
    private String githubRepo;
    private String githubBranch;
    private String workflowFile;
    private String webhookSecret;
    private boolean tokenConfigured;
    private String monitoredUrl;
    private String prometheusJobName;

    // NEW — safe to return as-is; unlike the GitHub token this isn't a secret,
    // just a webhook URL (though it's still sensitive-ish, so treat it the way
    // the UI already treats the webhook secret: visible but not advertised).
    private String deployHookUrl;

    public ProjectIntegrationResponse() {}

    public String getMonitoredUrl() {
        return monitoredUrl;
    }

    public String getPrometheusJobName() {
        return prometheusJobName;
    }

    public ProjectIntegrationResponse(Long id, Long projectId, String githubOwner, String githubRepo,
                                      String githubBranch, String workflowFile, String webhookSecret,
                                      boolean tokenConfigured, String deployHookUrl) {
        this.id = id;
        this.projectId = projectId;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.githubBranch = githubBranch;
        this.workflowFile = workflowFile;
        this.webhookSecret = webhookSecret;
        this.tokenConfigured = tokenConfigured;
        this.deployHookUrl = deployHookUrl;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getGithubOwner() { return githubOwner; }
    public String getGithubRepo() { return githubRepo; }
    public String getGithubBranch() { return githubBranch; }
    public String getWorkflowFile() { return workflowFile; }
    public String getWebhookSecret() { return webhookSecret; }
    public boolean isTokenConfigured() { return tokenConfigured; }
    public String getDeployHookUrl() { return deployHookUrl; }
}