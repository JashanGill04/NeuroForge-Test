package com.nexus.NeuroForge.controllers.Analytics;

import com.nexus.NeuroForge.dto.AnalyticsOverviewDTO;
import com.nexus.NeuroForge.services.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/project/{projectId}/overview")
    public ResponseEntity<AnalyticsOverviewDTO> getAnalyticsOverview(@PathVariable Long projectId) {
        return ResponseEntity.ok(analyticsService.getProjectAnalytics(projectId));
    }
}