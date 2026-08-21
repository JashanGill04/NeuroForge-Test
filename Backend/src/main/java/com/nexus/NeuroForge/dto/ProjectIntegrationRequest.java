package com.nexus.NeuroForge.dto;

public class ProjectIntegrationRequest {
    private String githubOwner;
    private String githubRepo;
    private String githubBranch;
    private String workflowFile;
    // Plaintext, only ever sent client -> server, never returned back.
    private String githubToken;

    private String monitoredUrl;
    private String metricsToken;
    private String prometheusJobName;

    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }
    public String getGithubRepo() { return githubRepo; }
    public void setGithubRepo(String githubRepo) { this.githubRepo = githubRepo; }
    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }
    public String getWorkflowFile() { return workflowFile; }
    public void setWorkflowFile(String workflowFile) { this.workflowFile = workflowFile; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }

    public String getMonitoredUrl() {
        return monitoredUrl;
    }

    public void setMonitoredUrl(String monitoredUrl) {
        this.monitoredUrl = monitoredUrl;
    }
    public String getMetricsToken() {
        return metricsToken;
    }

    public void setMetricsToken(String metricsToken) {
        this.metricsToken = metricsToken;
    }

    public String getPrometheusJobName() {
        return prometheusJobName;
    }

    public void setPrometheusJobName(String prometheusJobName) {
        this.prometheusJobName = prometheusJobName;
    }
}