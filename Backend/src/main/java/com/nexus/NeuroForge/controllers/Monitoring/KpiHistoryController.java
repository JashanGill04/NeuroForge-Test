package com.nexus.NeuroForge.controllers.Monitoring;

import com.nexus.NeuroForge.models.KpiSnapshot;
import com.nexus.NeuroForge.repositories.KpiSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/kpi-history")
public class KpiHistoryController {

    @Autowired private KpiSnapshotRepository snapshotRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KpiSnapshot>> getHistory(@RequestParam Long projectId,
                                                        @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(snapshotRepository.findByProjectIdAndCapturedAtAfterOrderByCapturedAtAsc(projectId, since));
    }
}