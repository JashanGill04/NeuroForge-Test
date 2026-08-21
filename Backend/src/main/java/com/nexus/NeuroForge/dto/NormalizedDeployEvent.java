// Backend/src/main/java/com/nexus/NeuroForge/dto/NormalizedDeployEvent.java
package com.nexus.NeuroForge.dto;

public class NormalizedDeployEvent {
    public String provider;
    public String status;      // "SUCCESS" | "FAILED" | "IN_PROGRESS"
    public String commitHash;
    public String deployId;
}