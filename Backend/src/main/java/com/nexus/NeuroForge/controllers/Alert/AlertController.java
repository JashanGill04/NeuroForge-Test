package com.nexus.NeuroForge.controllers.Alert;

import com.nexus.NeuroForge.dto.AlertRuleRequest;
import com.nexus.NeuroForge.models.Alert;
import com.nexus.NeuroForge.models.AlertRule;
import com.nexus.NeuroForge.services.AlertMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired private AlertMonitoringService alertMonitoringService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Alert>> getAlerts(@RequestParam Long projectId) {
        return ResponseEntity.ok(alertMonitoringService.getAllAlerts(projectId));
    }

    @GetMapping("/rules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlertRule>> getRules(@RequestParam Long projectId) {
        return ResponseEntity.ok(alertMonitoringService.getAllRules(projectId));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<AlertRule> createRule(@RequestParam Long projectId, @RequestBody AlertRuleRequest req) {
        return ResponseEntity.ok(alertMonitoringService.createRule(projectId, req));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<AlertRule> updateRule(@PathVariable Long id, @RequestBody AlertRuleRequest req) {
        return ResponseEntity.ok(alertMonitoringService.updateRule(id, req));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        alertMonitoringService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}