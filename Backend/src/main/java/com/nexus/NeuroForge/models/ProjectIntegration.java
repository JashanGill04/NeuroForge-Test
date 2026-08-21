package com.nexus.NeuroForge.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "project_integrations")
public class ProjectIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", unique = true, nullable = false)
    @JsonIgnore
    private Project project;

    private String githubOwner;
    private String githubRepo;
    private String githubBranch = "main";
    private String workflowFile = "ci-cd.yml";


    // Encrypted at rest — never serialized, never returned raw via API.
    @Column(name = "github_token_encrypted", columnDefinition = "TEXT")
    @JsonIgnore
    private String githubTokenEncrypted;

    // Given to the user to paste into their repo's Actions secrets.
    // Used to verify inbound webhook payloads (see WebhookSignatureValidator).
    @Column(name = "webhook_secret")
    private String webhookSecret;

    private String monitoredUrl="";       // e.g. https://your-app.onrender.com
    private String metricsToken="";
    private String prometheusJobName="";// matches METRICS_TOKEN on the Render side

    // NEW — generic "deploy hook" URL. Most hosting platforms (Render, Railway,
    // Fly.io, Vercel, Netlify, Heroku) expose a POST endpoint that redeploys the
    // latest build with zero platform-specific API integration required on our
    // side. CI calls this after a successful build+push so a real deploy actually
    // happens, instead of the CI run only exercising a throwaway container on the
    // runner itself. Optional — if blank, that step is just skipped.
    private String deployHookUrl = "";

    public String getDeployHookUrl() {
        return deployHookUrl;
    }

    public void setDeployHookUrl(String deployHookUrl) {
        this.deployHookUrl = deployHookUrl;
    }

    public String getPrometheusJobName() {
        return prometheusJobName;
    }

    public void setPrometheusJobName(String prometheusJobName) {
        this.prometheusJobName = prometheusJobName;
    }

    public String getMetricsToken() {
        return metricsToken;
    }

    public void setMetricsToken(String metricsToken) {
        this.metricsToken = metricsToken;
    }

    public String getMonitoredUrl() {
        return monitoredUrl;
    }

    public void setMonitoredUrl(String monitoredUrl) {
        this.monitoredUrl = monitoredUrl;
    }

    // e.g. "render-nodejs-backend" — must match prometheus.yml


    public ProjectIntegration() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getGithubOwner() { return githubOwner; }
    public void setGithubOwner(String githubOwner) { this.githubOwner = githubOwner; }
    public String getGithubRepo() { return githubRepo; }
    public void setGithubRepo(String githubRepo) { this.githubRepo = githubRepo; }
    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }
    public String getWorkflowFile() { return workflowFile; }
    public void setWorkflowFile(String workflowFile) { this.workflowFile = workflowFile; }
    public String getGithubTokenEncrypted() { return githubTokenEncrypted; }
    public void setGithubTokenEncrypted(String githubTokenEncrypted) { this.githubTokenEncrypted = githubTokenEncrypted; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}