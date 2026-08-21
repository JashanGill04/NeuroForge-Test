// PipelineWebhookRequest.java — [M3][Jashanpreet] payload GitHub Actions POSTs on build finish
package com.nexus.NeuroForge.dto;

import com.nexus.NeuroForge.models.interfaces.PipelineStatus;
import com.nexus.NeuroForge.models.interfaces.TriggerSource;
import com.nexus.NeuroForge.models.interfaces.PipelineStatus;
import com.nexus.NeuroForge.models.interfaces.StageStatus;
import com.nexus.NeuroForge.models.interfaces.TriggerSource;
import java.time.LocalDateTime;
import java.util.List;

public class PipelineWebhookRequest {
    private Long projectId;
    private PipelineStatus status;
    private int duration;
    private String commitHash;
    private String commitMessage;
    private String branch;
    private String environment;
    private boolean deploymentSuccess;
    private TriggerSource triggerSource;

    // NEW: the real wall-clock time the pipeline started, sent by the CI
    // workflow itself. Optional — if the caller doesn't send it (e.g. an
    // older webhook payload), PipelineService falls back to deriving it
    // from the stage durations like before.
    private LocalDateTime startedAt;

    private List<StageDTO> stages;
    private TestSummaryDTO testSummary;
    private DeploymentInfoDTO deploymentInfo;







    // getters/setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public PipelineStatus getStatus() { return status; }
    public void setStatus(PipelineStatus status) { this.status = status; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public boolean isDeploymentSuccess() { return deploymentSuccess; }
    public void setDeploymentSuccess(boolean deploymentSuccess) { this.deploymentSuccess = deploymentSuccess; }
    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }
    public TriggerSource getTriggerSource() { return triggerSource; }
    public void setTriggerSource(TriggerSource triggerSource) { this.triggerSource = triggerSource; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public List<StageDTO> getStages() { return stages; }
    public void setStages(List<StageDTO> stages) { this.stages = stages; }
    public TestSummaryDTO getTestSummary() { return testSummary; }
    public void setTestSummary(TestSummaryDTO testSummary) { this.testSummary = testSummary; }
    public DeploymentInfoDTO getDeploymentInfo() { return deploymentInfo; }
    public void setDeploymentInfo(DeploymentInfoDTO deploymentInfo) { this.deploymentInfo = deploymentInfo; }





    public static class StageDTO {
        public String name;
        public StageStatus status;
        public int durationSeconds;
    }

  public static class TestSummaryDTO {
    public int passed;
    public int failed;
    public int skipped;
    public double coveragePercent;
}

    public static class DeploymentInfoDTO {
        public String imageTag;
        public int podsRunning;
        public int podsTotal;
        public double cpuPercent;
        public double memoryPercent;
    }
}