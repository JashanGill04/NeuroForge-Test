package com.nexus.NeuroForge.controllers.Sprint;

import com.nexus.NeuroForge.dto.SprintRequest;
import com.nexus.NeuroForge.dto.SprintResponse;
import com.nexus.NeuroForge.services.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    @Autowired
    private SprintService sprintService;

    @PostMapping("/plan")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<SprintResponse> planSprint(@RequestBody SprintRequest req) {
        return ResponseEntity.ok(sprintService.planSprint(req));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintResponse>> getProjectSprints(@PathVariable Long projectId) {
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectId));
    }
}