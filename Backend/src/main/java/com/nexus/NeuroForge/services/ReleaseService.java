// ReleaseService.java — [M4][Jashanpreet]
package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.*;
import com.nexus.NeuroForge.models.Deployment;
import com.nexus.NeuroForge.models.Release;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.models.interfaces.DeploymentSlot;
import com.nexus.NeuroForge.models.interfaces.ReleaseStatus;
import com.nexus.NeuroForge.repositories.DeploymentRepository;
import com.nexus.NeuroForge.repositories.HealthCheckResultRepository;
import com.nexus.NeuroForge.repositories.MonitoringTargetRepository;
import com.nexus.NeuroForge.repositories.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReleaseService {

    @Autowired private ReleaseRepository releaseRepository;
    @Autowired private DeploymentRepository deploymentRepository;

    @Autowired private HealthCheckResultRepository healthCheckResultRepository;
    @Autowired private MonitoringTargetRepository monitoringTargetRepository;

    // PipelineService already owns the GitHub Actions dispatch + rollback-eligibility
    // logic from M3 (executeRollback). ReleaseService reuses it rather than
    // re-implementing the workflow trigger, and layers the release-record
    // bookkeeping (blue-green slot swap, status transitions) on top.
    @Autowired private PipelineService pipelineService;

    // -------------------------------------------------------------------
    // Project-scoped lookup helpers.
    //
    // A Release's Deployment can be linked to its Project two ways: via a
    // Pipeline (the original CI-triggered flow), or directly (deployments
    // recorded straight from a hosting provider's webhook — see
    // DeployWebhookController). Since Step 1, *every* new Deployment sets
    // the direct `project` link regardless of which path created it, so a
    // naive "query both and concat" would double-count CI-originated
    // releases. These helpers merge by Release id instead, so each release
    // shows up exactly once no matter which path recorded its deployment.
    // -------------------------------------------------------------------

    private List<Release> mergeById(List<Release> a, List<Release> b) {
        Map<Long, Release> byId = new LinkedHashMap<>();
        for (Release r : a) byId.put(r.getId(), r);
        for (Release r : b) byId.put(r.getId(), r);
        List<Release> merged = new ArrayList<>(byId.values());
        merged.sort(Comparator.comparing(Release::getReleaseDate).reversed());
        return merged;
    }

    private List<Release> findAllForProject(Long projectId) {
        return mergeById(
                releaseRepository.findByDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(projectId),
                releaseRepository.findByDeployment_Project_IdOrderByReleaseDateDesc(projectId)
        );
    }

    private List<Release> findForProjectEnvironment(Long projectId, DeploymentEnvironment env) {
        return mergeById(
                releaseRepository.findByEnvironmentAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(env, projectId),
                releaseRepository.findByEnvironmentAndDeployment_Project_IdOrderByReleaseDateDesc(env, projectId)
        );
    }

    private Optional<Release> findActiveForProjectEnvironment(Long projectId, DeploymentEnvironment env) {
        // At most one release is ever "active" per project/environment (createRelease
        // and rollbackRelease enforce that), so both queries can return at most one row
        // each and they can't disagree — just take whichever is present. Preferring
        // the direct-project query since it's the superset going forward.
        Optional<Release> viaDirect = releaseRepository
                .findTopByEnvironmentAndActiveTrueAndDeployment_Project_IdOrderByReleaseDateDesc(env, projectId);
        if (viaDirect.isPresent()) return viaDirect;
        return releaseRepository
                .findTopByEnvironmentAndActiveTrueAndDeployment_Pipeline_Project_IdOrderByReleaseDateDesc(env, projectId);
    }

    /**
     * Cuts a new Release from a successful Deployment and promotes it to
     * live traffic in its environment (the "green" side goes live, the
     * previously active release becomes the standby "blue" side, or vice
     * versa). Scoped per-project: the "currently active" lookup for the
     * blue-green swap only considers releases belonging to the SAME
     * project as the deployment being released, so two projects deploying
     * to the same environment name (e.g. both to STAGING) never step on
     * each other's active release.
     *
     * Works identically for deployments that came from a CI Pipeline OR
     * straight from a hosting provider's deploy webhook — both are just "a
     * Deployment row belonging to a project" here (see
     * Deployment.resolveProjectId()).
     */
    public Release createRelease(CreateReleaseRequest req) {
        Deployment deployment = deploymentRepository.findById(req.getDeploymentId())
                .orElseThrow(() -> new IllegalArgumentException("No deployment found with id " + req.getDeploymentId()));

        if (!deployment.isSuccess()) {
            throw new IllegalStateException("Cannot cut a release from a deployment that did not succeed.");
        }

        if (releaseRepository.findByDeployment_Id(deployment.getId()).isPresent()) {
            throw new IllegalStateException("A release already exists for this deployment.");
        }

        Long projectId = deployment.resolveProjectId();
        if (projectId == null) {
            throw new IllegalStateException("Deployment " + deployment.getId()
                    + " isn't linked to a project or a pipeline — cannot determine which project to release under.");
        }

        DeploymentEnvironment env = deployment.getEnvironment();
        Optional<Release> currentlyActive = findActiveForProjectEnvironment(projectId, env);

        Release release = new Release();
        release.setDeployment(deployment);
        release.setVersion(deployment.getImageTag() != null ? deployment.getImageTag() : "deploy-" + deployment.getId());
        release.setApproved(req.isApproved());
        release.setReleaseDate(LocalDateTime.now());
        release.setEnvironment(env);
        release.setStatus(ReleaseStatus.DEPLOYED);
        release.setActive(true);
        release.setSlot(currentlyActive
                .map(r -> r.getSlot() == DeploymentSlot.BLUE ? DeploymentSlot.GREEN : DeploymentSlot.BLUE)
                .orElse(DeploymentSlot.BLUE));

        releaseRepository.save(release);

        // Blue-green swap: the old active release stands down but stays in
        // history (not rolled back) so it can be reactivated instantly if
        // this new one needs to be rolled back later.
        currentlyActive.ifPresent(prev -> {
            prev.setActive(false);
            prev.setStatus(ReleaseStatus.SUPERSEDED);
            releaseRepository.save(prev);
        });

        return release;
    }

    /**
     * Rolls back the currently active release in its environment: triggers
     * the actual GitHub Actions rollback workflow via PipelineService (same
     * mechanism M3 already validates), then flips the blue-green slots back
     * so the previous release becomes active again. The "previous release"
     * lookup is scoped to the same project as the release being rolled
     * back, for the same isolation reason as createRelease above.
     *
     * NOTE: this only works for releases whose Deployment came from a
     * NeuroForge-triggered CI Pipeline run — that's the only path with a
     * stored previous image and a workflow to re-dispatch. Releases
     * recorded directly from a hosting provider's deploy webhook (no
     * Pipeline attached) can't be rolled back through this mechanism yet;
     * roll those back from the provider's own dashboard.
     */
    public void rollbackRelease(Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IllegalArgumentException("No release found with id " + releaseId));

        if (!release.isActive()) {
            throw new IllegalStateException("Only the currently active release can be rolled back.");
        }

        Deployment deployment = release.getDeployment();
        if (deployment.getPipeline() == null) {
            throw new IllegalStateException(
                    "This release wasn't created from a NeuroForge-triggered CI build, so there's no pipeline "
                            + "to re-dispatch for an automated rollback. Roll it back from your hosting provider directly.");
        }

        Long pipelineId = deployment.getPipeline().getId();
        Long projectId = deployment.resolveProjectId();

        pipelineService.executeRollback(pipelineId); // dispatches the real rollback workflow

        release.setActive(false);
        release.setStatus(ReleaseStatus.ROLLED_BACK);
        releaseRepository.save(release);

        // Reactivate the most recently superseded release in the same
        // environment AND same project — that's the image the rollback
        // workflow just redeployed.
        findForProjectEnvironment(projectId, release.getEnvironment()).stream()
                .filter(r -> r.getStatus() == ReleaseStatus.SUPERSEDED)
                .findFirst()
                .ifPresent(prev -> {
                    prev.setActive(true);
                    prev.setStatus(ReleaseStatus.DEPLOYED);
                    releaseRepository.save(prev);
                });
    }

    public Release getActiveRelease(Long projectId, DeploymentEnvironment environment) {
        return findActiveForProjectEnvironment(projectId, environment)
                .orElseThrow(() -> new IllegalStateException("No active release for environment " + environment));
    }

    public List<ReleaseResponse> getHistory(Long projectId) {
        return findAllForProject(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReleaseDetailDTO getDetail(Long id) {
        Release r = releaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No release found with id " + id));

        ReleaseDetailDTO dto = new ReleaseDetailDTO();
        dto.id = r.getId();
        dto.version = r.getVersion();
        dto.environment = r.getEnvironment() != null ? r.getEnvironment().name() : null;
        dto.status = r.getStatus() != null ? r.getStatus().name() : null;
        dto.slot = r.getSlot() != null ? r.getSlot().name() : null;
        dto.active = r.isActive();
        dto.approved = r.isApproved();
        dto.releaseDate = r.getReleaseDate();

        Deployment d = r.getDeployment();
        if (d != null) {
            var di = new ReleaseDetailDTO.DeploymentInfo();
            di.id = d.getId();
            di.imageTag = d.getImageTag();
            di.podsRunning = d.getPodsRunning();
            di.podsTotal = d.getPodsTotal();
            di.cpuPercent = d.getCpuPercent();
            di.memoryPercent = d.getMemoryPercent();
            di.success = d.isSuccess();
            dto.deployment = di;

            if (d.getPipeline() != null) {
                var pi = new ReleaseDetailDTO.PipelineInfo();
                pi.id = d.getPipeline().getId();
                pi.branch = d.getPipeline().getBranch();
                pi.commitHash = d.getPipeline().getCommitHash();
                pi.commitMessage = d.getPipeline().getCommitMessage();
                dto.pipeline = pi;
            }
        }

        return dto;
    }

    /**
     * KPIs are cached per project so a scrape / dashboard burst only
     * recomputes once per project every few seconds, while still surfacing
     * new releases/rollbacks almost immediately.
     */
    private final Map<Long, ReleaseKpiDTO> cachedKpis = new ConcurrentHashMap<>();
    private final Map<Long, Long> cachedKpisAt = new ConcurrentHashMap<>();
    private static final long KPI_CACHE_MS = 5000L;

    public ReleaseKpiDTO getKpis(Long projectId) {
        long now = System.currentTimeMillis();
        Long lastAt = cachedKpisAt.get(projectId);
        if (lastAt != null && (now - lastAt) < KPI_CACHE_MS) {
            ReleaseKpiDTO cached = cachedKpis.get(projectId);
            if (cached != null) {
                return cached;
            }
        }
        ReleaseKpiDTO fresh = computeKpis(projectId);
        cachedKpis.put(projectId, fresh);
        cachedKpisAt.put(projectId, now);
        return fresh;
    }

    private ReleaseKpiDTO computeKpis(Long projectId) {
        List<Release> all = findAllForProject(projectId);
        long total = all.size();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        long releasesThisMonth = all.stream()
                .filter(r -> !r.getReleaseDate().isBefore(monthStart) && !r.getReleaseDate().isAfter(now))
                .count();

        List<Release> rolledBack = all.stream()
                .filter(r -> r.getStatus() == ReleaseStatus.ROLLED_BACK)
                .collect(Collectors.toList());
        long rolledBackCount = rolledBack.size();

        double mttrMinutes = rolledBack.stream()
                .mapToDouble(r -> minutesToRecovery(r, projectId))
                .filter(m -> m >= 0)
                .average()
                .orElse(0);

        double incidentRate = total == 0 ? 0 : (double) rolledBackCount / total;
        double uptimePercent = computeRealOrSimulatedUptime(projectId, incidentRate);

        return new ReleaseKpiDTO(releasesThisMonth, round(uptimePercent), round(mttrMinutes), total, rolledBackCount);
    }

    /**
     * Prefers real uptime from HealthCheckResult (populated by
     * ExternalHealthMonitorService polling every enabled MonitoringTarget
     * for this project, regardless of which Prober strategy produced the
     * rows). Falls back to the simulated release/rollback-derived formula
     * for projects that haven't configured a monitoring target yet, or
     * haven't accumulated any probe history in the last 24h.
     */
    private double computeRealOrSimulatedUptime(Long projectId, double incidentRate) {
        boolean hasMonitoringTarget = !monitoringTargetRepository
                .findByProject_IdAndEnabledTrue(projectId)
                .isEmpty();

        if (!hasMonitoringTarget) {
            return Math.max(0, 100.0 - (incidentRate * 5.0));
        }

        LocalDateTime windowStart = LocalDateTime.now().minusHours(24);
        long total = healthCheckResultRepository.countByProjectIdAndCheckedAtAfter(projectId, windowStart);
        if (total == 0) {
            return Math.max(0, 100.0 - (incidentRate * 5.0));
        }

        long up = healthCheckResultRepository.countByProjectIdAndUpTrueAndCheckedAtAfter(projectId, windowStart);
        return round(100.0 * up / total);
    }

    // Time between a rolled-back release going live and the replacement
    // release (the one that superseded it, redeployed after rollback)
    // going live — i.e. how long the bad release was serving traffic.
    // Scoped to the same project as the rolled-back release.
    private double minutesToRecovery(Release rolledBackRelease, Long projectId) {
        return findForProjectEnvironment(projectId, rolledBackRelease.getEnvironment()).stream()
                .filter(r -> r.getReleaseDate().isAfter(rolledBackRelease.getReleaseDate()))
                .min(Comparator.comparing(Release::getReleaseDate))
                .map(next -> (double) Duration.between(rolledBackRelease.getReleaseDate(), next.getReleaseDate()).toMinutes())
                .orElse(-1.0);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public ReleaseResponse toResponse(Release r) {
        Deployment d = r.getDeployment();
        return new ReleaseResponse(
                r.getId(), r.getVersion(),
                r.getEnvironment() != null ? r.getEnvironment().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getSlot() != null ? r.getSlot().name() : null,
                r.isActive(), r.isApproved(), r.getReleaseDate(),
                d != null ? d.getId() : null,
                d != null && d.getPipeline() != null ? d.getPipeline().getId() : null
        );
    }

    /**
     * Platform-wide KPIs, aggregated across ALL projects — used by
     * ObservabilityConfig's Prometheus gauges, which are single global values
     * and can't be parameterized per scrape. Per-project KPIs (used by the
     * dashboard UI) go through getKpis(Long projectId) instead. Kept as the
     * simulated formula since "real uptime, aggregated across every
     * project's targets" isn't a single meaningful number the way per-project
     * uptime is. This one is unaffected by the pipeline/direct split since
     * findAllByOrderByReleaseDateDesc() already returns every release exactly
     * once regardless of how its deployment was linked.
     */
    public ReleaseKpiDTO getPlatformKpis() {
        long now = System.currentTimeMillis();
        if (cachedPlatformKpis != null && (now - cachedPlatformKpisAt) < KPI_CACHE_MS) {
            return cachedPlatformKpis;
        }
        ReleaseKpiDTO fresh = computePlatformKpis();
        cachedPlatformKpis = fresh;
        cachedPlatformKpisAt = now;
        return fresh;
    }

    private volatile ReleaseKpiDTO cachedPlatformKpis;
    private volatile long cachedPlatformKpisAt = 0L;

    private ReleaseKpiDTO computePlatformKpis() {
        List<Release> all = releaseRepository.findAllByOrderByReleaseDateDesc();
        long total = all.size();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long releasesThisMonth = releaseRepository.countByReleaseDateBetween(monthStart, LocalDateTime.now());

        List<Release> rolledBack = all.stream()
                .filter(r -> r.getStatus() == ReleaseStatus.ROLLED_BACK)
                .collect(Collectors.toList());
        long rolledBackCount = rolledBack.size();

        double mttrMinutes = rolledBack.stream()
                .mapToDouble(this::minutesToRecoveryPlatformWide)
                .filter(m -> m >= 0)
                .average()
                .orElse(0);

        double incidentRate = total == 0 ? 0 : (double) rolledBackCount / total;
        double uptimePercent = Math.max(0, 100.0 - (incidentRate * 5.0));

        return new ReleaseKpiDTO(releasesThisMonth, round(uptimePercent), round(mttrMinutes), total, rolledBackCount);
    }

    private double minutesToRecoveryPlatformWide(Release rolledBackRelease) {
        return releaseRepository.findByEnvironmentOrderByReleaseDateDesc(rolledBackRelease.getEnvironment()).stream()
                .filter(r -> r.getReleaseDate().isAfter(rolledBackRelease.getReleaseDate()))
                .min(Comparator.comparing(Release::getReleaseDate))
                .map(next -> (double) Duration.between(rolledBackRelease.getReleaseDate(), next.getReleaseDate()).toMinutes())
                .orElse(-1.0);
    }
}