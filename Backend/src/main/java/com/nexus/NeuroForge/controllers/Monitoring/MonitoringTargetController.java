// Backend/src/main/java/com/nexus/NeuroForge/controllers/MonitoringTargetController.java
package com.nexus.NeuroForge.controllers.Monitoring;

import com.nexus.NeuroForge.dto.MonitoringTargetRequest;
import com.nexus.NeuroForge.dto.MonitoringTargetResponse;
import com.nexus.NeuroForge.services.MonitoringTargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/monitoring-targets")
public class MonitoringTargetController {

    @Autowired private MonitoringTargetService monitoringTargetService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MonitoringTargetResponse>> getAll(@PathVariable Long projectId) {
        return ResponseEntity.ok(monitoringTargetService.getByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<MonitoringTargetResponse> create(@PathVariable Long projectId, @RequestBody MonitoringTargetRequest req) {
        return ResponseEntity.ok(monitoringTargetService.create(projectId, req));
    }


    // MonitoringTargetController.java — add
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<MonitoringTargetResponse> update(
            @PathVariable Long projectId, @PathVariable Long id, @RequestBody MonitoringTargetRequest req) {
        return ResponseEntity.ok(monitoringTargetService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        monitoringTargetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}