package com.nexus.NeuroForge.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

// [M3][Jashanpreet] Deployment entity — one deploy attempt to one environment.
// Linked TO: Pipeline (N:1, owning side, now OPTIONAL), Project (N:1, direct — used
// when a deployment arrives from an external host webhook with no CI pipeline behind it),
// Release (1:1)

import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Deployment {

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public int getPodsRunning() {
        return podsRunning;
    }

    public void setPodsRunning(int podsRunning) {
        this.podsRunning = podsRunning;
    }

    public int getPodsTotal() {
        return podsTotal;
    }

    public void setPodsTotal(int podsTotal) {
        this.podsTotal = podsTotal;
    }

    public double getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(double cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public double getMemoryPercent() {
        return memoryPercent;
    }

    public void setMemoryPercent(double memoryPercent) {
        this.memoryPercent = memoryPercent;
    }

    public boolean isRollbackEligible() {
        return rollbackEligible;
    }

    public void setRollbackEligible(boolean rollbackEligible) {
        this.rollbackEligible = rollbackEligible;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // add these fields alongside existing ones
    private String imageTag;
    private int podsRunning;
    private int podsTotal;
    private double cpuPercent;
    private double memoryPercent;
    private boolean rollbackEligible;

    @Enumerated(EnumType.STRING)
    private DeploymentEnvironment environment;

    private boolean success;

    private LocalDateTime deployedAt;

    // OPTIONAL now — deployments coming from a CI pipeline (existing GitHub Actions
    // flow) set this. Deployments coming straight from a hosting provider's webhook
    // (Render/Railway/Fly/etc. — no CI run involved) leave this null and set
    // `project` directly instead. See resolveProjectId() below for the one place
    // that needs to handle both cases.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    @JsonIgnore
    private Pipeline pipeline;

    // NEW — direct project link, used when there's no Pipeline behind this deployment.
    // Always set this going forward (PipelineService now sets it too), so every new
    // Deployment row has an unambiguous project owner regardless of how it arrived.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @OneToOne(mappedBy = "deployment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Release release;

    public Deployment() {}

    public Deployment(Long id, DeploymentEnvironment environment, boolean success) {
        this.id = id;
        this.environment = environment;
        this.success = success;
    }

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DeploymentEnvironment getEnvironment() { return environment; }
    public void setEnvironment(DeploymentEnvironment environment) { this.environment = environment; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public LocalDateTime getDeployedAt() { return deployedAt; }
    public void setDeployedAt(LocalDateTime deployedAt) { this.deployedAt = deployedAt; }
    public Pipeline getPipeline() { return pipeline; }
    public void setPipeline(Pipeline pipeline) { this.pipeline = pipeline; }
    public Release getRelease() { return release; }
    public void setRelease(Release release) { this.release = release; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    /**
     * The one place that needs to know a Deployment's project regardless of
     * whether it came from a CI pipeline or a direct host webhook. Prefers the
     * direct link (set on every new Deployment now); falls back to walking
     * through Pipeline for older rows / the existing CI flow if `project`
     * somehow wasn't set.
     */
    public Long resolveProjectId() {
        if (project != null) {
            return project.getId();
        }
        if (pipeline != null && pipeline.getProject() != null) {
            return pipeline.getProject().getId();
        }
        return null;
    }
}