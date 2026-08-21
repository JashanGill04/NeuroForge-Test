// Backend/src/main/java/com/nexus/NeuroForge/models/HealthCheckResult.java
package com.nexus.NeuroForge.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_check_results")
public class HealthCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;
    private boolean up;
    private Long responseTimeMs;
    private LocalDateTime checkedAt;
    // add to HealthCheckResult.java
    private Long targetId;

    public HealthCheckResult() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public boolean isUp() { return up; }
    public void setUp(boolean up) { this.up = up; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
}