package com.nexus.NeuroForge.repositories;

import com.nexus.NeuroForge.models.Release;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.interfaces.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    Optional<Release> findByDeployment_Id(Long deploymentId);

    Optional<Release> findTopByEnvironmentAndActiveTrueOrderByReleaseDateDesc(DeploymentEnvironment environment);

    List<Release> findByEnvironmentOrderByReleaseDateDesc(DeploymentEnvironment environment);

    List<Release> findByStatusOrderByReleaseDateDesc(ReleaseStatus status);

    List<Release> findAllByOrderByReleaseDateDesc();


    // --- Deployments that came from a NeuroForge-triggered CI run (existing flow) ---

    List<Release> findByDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(Long projectId);

    Optional<Release> findTopByEnvironmentAndActiveTrueAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(
            DeploymentEnvironment environment, Long projectId);

    List<Release> findByEnvironmentAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(
            DeploymentEnvironment environment, Long projectId);

    long countByReleaseDateBetweenAndDeployment_Pipeline_Project_Id(
            LocalDateTime start, LocalDateTime end, Long projectId);


    // --- NEW: deployments that came straight from a hosting provider's webhook
    //     (no Pipeline attached) — see Deployment.project / resolveProjectId() ---

    List<Release> findByDeployment_Project_IdOrderByReleaseDateDesc(Long projectId);

    Optional<Release> findTopByEnvironmentAndActiveTrueAndDeployment_Project_IdOrderByReleaseDateDesc(
            DeploymentEnvironment environment, Long projectId);

    List<Release> findByEnvironmentAndDeployment_Project_IdOrderByReleaseDateDesc(
            DeploymentEnvironment environment, Long projectId);


    long countByReleaseDateBetween(LocalDateTime start, LocalDateTime end);
}