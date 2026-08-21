package com.nexus.NeuroForge.controllers.Sprint;

import com.nexus.NeuroForge.dto.SprintProgressDTO;
import com.nexus.NeuroForge.services.SprintProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sprint-progress")
public class SprintProgressController {

    @Autowired
    private SprintProgressService sprintProgressService;

    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintProgressDTO> getProgress(@PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintProgressService.getSprintProgress(sprintId));
    }
}