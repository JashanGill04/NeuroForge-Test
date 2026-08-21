// PipelineService.java — [M3][Jashanpreet]
package com.nexus.NeuroForge.services;

import com.nexus.NeuroForge.dto.*;
import com.nexus.NeuroForge.models.*;
import com.nexus.NeuroForge.models.interfaces.DeploymentEnvironment;
import com.nexus.NeuroForge.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.nexus.NeuroForge.models.interfaces.TestResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PipelineService {

    @Autowired private PipelineRepository pipelineRepository;
    @Autowired private DeploymentRepository deploymentRepository;
    @Autowired private ProjectRepository projectRepository; // assumes this exists already

    // Called by the webhook when GitHub Actions finishes a build
public Pipeline recordBuildResult(PipelineWebhookRequest req) {
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("No project found with id " + req.getProjectId()));

        Pipeline pipeline = new Pipeline();
        pipeline.setStatus(req.getStatus());
        pipeline.setCommitHash(req.getCommitHash());
        pipeline.setCommitMessage(req.getCommitMessage());
        pipeline.setBranch(req.getBranch());
        pipeline.setTriggerSource(req.getTriggerSource());
        
        // --- DURATION CALCULATION ---
        // Total duration is still the sum of the individual stage durations,
        // but those durations are now measured by the CI workflow itself
        // (real elapsed seconds per stage) rather than hardcoded constants.
        int totalDuration = 0;
        if (req.getStages() != null) {
            totalDuration = req.getStages().stream()
                    .mapToInt(s -> s.durationSeconds)
                    .sum();
        }

        pipeline.setDuration(totalDuration);

        // Snapshot "now" exactly once so finishedAt and any fallback math
        // below are internally consistent instead of drifting between two
        // separate now() calls.
        LocalDateTime finishedAt = LocalDateTime.now();
        pipeline.setFinishedAt(finishedAt);

        // FIX: prefer the real pipeline start time the CI workflow sends.
        // Only fall back to reconstructing it from totalDuration if the
        // caller didn't provide one (e.g. an older/manual webhook call) —
        // previously this backdating was the *only* path, which made
        // startedAt a pure function of totalDuration and meant
        // finishedAt - startedAt always trivially equaled the (fake)
        // duration instead of reflecting a real elapsed time.
        if (req.getStartedAt() != null) {
            pipeline.setStartedAt(req.getStartedAt());
        } else {
            pipeline.setStartedAt(finishedAt.minusSeconds(totalDuration));
        }
        // --------------------------------------

        pipeline.setProject(project);



        if (req.getStages() != null) {
            int order = 0;
            for (var s : req.getStages()) {
                PipelineStage stage = new PipelineStage();
                stage.setName(s.name);
                stage.setStatus(s.status);
                stage.setDurationSeconds(s.durationSeconds);
                stage.setSequenceOrder(order++);
                stage.setPipeline(pipeline);
                pipeline.getStages().add(stage);
            }
        }

        if (req.getTestSummary() != null) {
            var ts = req.getTestSummary();
            for (int i = 0; i < ts.passed; i++) {
                TestCase tc = new TestCase();
                tc.setResult(TestResult.PASSED);
                tc.setCoverage(ts.coveragePercent);
                tc.setPipeline(pipeline);
                pipeline.getTestCases().add(tc);
            }
            for (int i = 0; i < ts.failed; i++) {
                TestCase tc = new TestCase();
                tc.setResult(TestResult.FAILED);
                tc.setCoverage(ts.coveragePercent);
                tc.setPipeline(pipeline);
                pipeline.getTestCases().add(tc);
            }
            for (int i = 0; i < ts.skipped; i++) {
                TestCase tc = new TestCase();
                tc.setResult(TestResult.SKIPPED);
                tc.setCoverage(ts.coveragePercent);
                tc.setPipeline(pipeline);
                pipeline.getTestCases().add(tc);
            }
        }

        Deployment deployment = new Deployment();
        deployment.setEnvironment(DeploymentEnvironment.valueOf(req.getEnvironment()));
        deployment.setSuccess(req.isDeploymentSuccess());
        deployment.setDeployedAt(LocalDateTime.now());
        deployment.setPipeline(pipeline);
    deployment.setProject(project);

        if (req.getDeploymentInfo() != null) {
            var di = req.getDeploymentInfo();
            deployment.setImageTag(di.imageTag);
            deployment.setPodsRunning(di.podsRunning);
            deployment.setPodsTotal(di.podsTotal);
            deployment.setCpuPercent(di.cpuPercent);
            deployment.setMemoryPercent(di.memoryPercent);
        }

        boolean hadPriorSuccess = deploymentRepository
                .existsByPipeline_Project_IdAndEnvironmentAndSuccessTrue(req.getProjectId(),
                        DeploymentEnvironment.valueOf(req.getEnvironment()));
        deployment.setRollbackEligible(hadPriorSuccess);

        pipeline.getDeployments().add(deployment);
        return pipelineRepository.save(pipeline);
    }

    // New — powers the detail view
    public PipelineDetailDTO getDetail(Long id) {
        Pipeline p = pipelineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No pipeline found with id " + id));

        PipelineDetailDTO dto = new PipelineDetailDTO();
        dto.id = p.getId();
        dto.status = p.getStatus().name();
        dto.branch = p.getBranch();
        dto.commitHash = p.getCommitHash();
        dto.commitMessage = p.getCommitMessage();
        dto.triggerSource = p.getTriggerSource() != null ? p.getTriggerSource().name() : null;
        dto.duration = p.getDuration();
        dto.startedAt = p.getStartedAt();
        dto.finishedAt = p.getFinishedAt();

        dto.stages = p.getStages().stream().map(s -> {
            var si = new PipelineDetailDTO.StageInfo();
            si.name = s.getName();
            si.status = s.getStatus().name();
            si.durationSeconds = s.getDurationSeconds();
            return si;
        }).collect(Collectors.toList());

        var testInfo = new PipelineDetailDTO.TestInfo();
        testInfo.passed = (int) p.getTestCases().stream()
                .filter(t -> t.getResult() == TestResult.PASSED).count();
        testInfo.failed = (int) p.getTestCases().stream()
                .filter(t -> t.getResult() == TestResult.FAILED).count();
        testInfo.skipped = (int) p.getTestCases().stream()
                .filter(t -> t.getResult() == TestResult.SKIPPED).count();
        testInfo.coveragePercent = p.getTestCases().stream()
                .mapToDouble(TestCase::getCoverage).average().orElse(0);
        dto.tests = testInfo;

        if (!p.getDeployments().isEmpty()) {
            Deployment d = p.getDeployments().get(p.getDeployments().size() - 1);
            var di = new PipelineDetailDTO.DeployInfo();
            di.id = d.getId();   // NEW
            di.environment = d.getEnvironment().name();
            di.success = d.isSuccess();
            di.imageTag = d.getImageTag();
            di.podsRunning = d.getPodsRunning();
            di.podsTotal = d.getPodsTotal();
            di.cpuPercent = d.getCpuPercent();
            di.memoryPercent = d.getMemoryPercent();
            di.rollbackEligible = d.isRollbackEligible();
            dto.deployment = di;
        }

        return dto;
    }

    public List<PipelineResponse> getHistory(Long projectId) {
        return pipelineRepository.findByProject_IdOrderByStartedAtDesc(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PipelineKpiDTO getKpis(Long projectId) {
        List<Pipeline> all = pipelineRepository.findByProject_IdOrderByStartedAtDesc(projectId);
        long total = all.size();
        long successCount = all.stream()
                .filter(p -> p.getStatus().name().equals("SUCCESS"))
                .count();
        double successRate = total == 0 ? 0 : (successCount * 100.0) / total;
        double avgDuration = all.stream().mapToInt(Pipeline::getDuration).average().orElse(0) / 60.0;
        long today = all.stream()
                .filter(p -> p.getFinishedAt() != null && p.getFinishedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        return new PipelineKpiDTO(total, successRate, avgDuration, today);
    }

    private PipelineResponse toResponse(Pipeline p) {
        Deployment d = p.getDeployments().isEmpty() ? null : p.getDeployments().get(p.getDeployments().size() - 1);
        return new PipelineResponse(
                p.getId(), p.getStatus().name(), p.getDuration(), p.getCommitHash(), p.getBranch(),
                p.getStartedAt(), p.getFinishedAt(),
                d != null ? d.getEnvironment().name() : null,
                d != null && d.isSuccess()
        );
    }

    // --- GitHub Actions integration (replaces the old Jenkins trigger/rollback stubs) ---

    @Autowired private RestTemplate restTemplate;

    @Autowired private ProjectIntegrationService projectIntegrationService;

    public void triggerJenkinsBuild(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No project found with id " + projectId));

        var integration = projectIntegrationService.getEntityOrThrow(projectId);

        dispatchWorkflow(integration, Map.of(
                "rollback", "false",
                "image_tag", ""
        ));
    }

    public void executeRollback(Long pipelineId) {
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new IllegalArgumentException("Pipeline not found"));

        if (pipeline.getDeployments().isEmpty()) {
            throw new IllegalStateException("This pipeline has no deployment to roll back.");
        }

        Deployment latest = pipeline.getDeployments().get(pipeline.getDeployments().size() - 1);
        if (!latest.isRollbackEligible()) {
            throw new IllegalStateException("This deployment is not eligible for rollback.");
        }

        DeploymentEnvironment env = latest.getEnvironment();
        Deployment previousGood = deploymentRepository
                .findTopByPipeline_Project_IdAndEnvironmentAndSuccessTrueAndPipeline_IdNotOrderByDeployedAtDesc(
                        pipeline.getProject().getId(), env, pipeline.getId())
                .orElseThrow(() -> new IllegalStateException("No previous successful deployment found to roll back to."));

        var integration = projectIntegrationService.getEntityOrThrow(pipeline.getProject().getId());

        dispatchWorkflow(integration, Map.of(
                "rollback", "true",
                "image_tag", previousGood.getImageTag()
        ));
    }

    private void dispatchWorkflow(com.nexus.NeuroForge.models.ProjectIntegration integration, Map<String, String> inputs) {
        String url = String.format(
                "https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches",
                integration.getGithubOwner(), integration.getGithubRepo(), integration.getWorkflowFile());

        String token = projectIntegrationService.decryptToken(integration);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of("ref", integration.getGithubBranch(), "inputs", inputs);
        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }

    /**
     * Platform-wide KPIs, aggregated across ALL projects — used by
     * AlertMonitoringService / KpiSnapshotScheduler, which need one global
     * reading rather than a per-project one. Per-project KPIs (dashboard UI)
     * still go through getKpis(Long projectId).
     */
    public PipelineKpiDTO getPlatformKpis() {
        long now = System.currentTimeMillis();
        if (cachedPlatformKpis != null && (now - cachedPlatformKpisAt) < KPI_CACHE_MS) {
            return cachedPlatformKpis;
        }
        PipelineKpiDTO fresh = computePlatformKpis();
        cachedPlatformKpis = fresh;
        cachedPlatformKpisAt = now;
        return fresh;
    }

    private volatile PipelineKpiDTO cachedPlatformKpis;
    private volatile long cachedPlatformKpisAt = 0L;
    private static final long KPI_CACHE_MS = 60_000; // match ReleaseService's cache window

    private PipelineKpiDTO computePlatformKpis() {
        List<Pipeline> all = pipelineRepository.findAllByOrderByStartedAtDesc();
        long total = all.size();

        long successCount = all.stream()
                .filter(p -> p.getStatus().name().equals("SUCCESS"))
                .count();
        double successRate = total == 0 ? 0 : (successCount * 100.0) / total;

        double avgDuration = all.stream().mapToInt(Pipeline::getDuration).average().orElse(0) / 60.0;

        long today = all.stream()
                .filter(p -> p.getFinishedAt() != null && p.getFinishedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        return new PipelineKpiDTO(total, successRate, avgDuration, today);
    }
}