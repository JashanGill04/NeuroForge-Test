package com.nexus.NeuroForge.controllers.Blockers;

// import com.nexus.NeuroForge.events.TaskEvent;
import com.nexus.NeuroForge.models.Blocker;
import com.nexus.NeuroForge.models.Sprint;
import com.nexus.NeuroForge.models.Task;
import com.nexus.NeuroForge.repositories.BlockerRepository;
import com.nexus.NeuroForge.repositories.SprintRepository;
import com.nexus.NeuroForge.repositories.TaskRepository;
// import com.nexus.NeuroForge.services.KafkaProducerService;
import com.nexus.NeuroForge.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints/{sprintId}/blockers")
public class BlockerController {

    @Autowired
    private BlockerRepository blockerRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskRepository taskRepository; // Injected to update task status

    // @Autowired
    // private KafkaProducerService kafkaProducer;

    // NOTIFICATION FIX: KafkaConsumerService used to be the only place that turned a
    // TaskEvent into a saved Notification row. With Kafka disabled that consumer never
    // runs, so notification creation for blocker events is now delegated directly to
    // NotificationService instead.
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public List<Blocker> getBlockers(@PathVariable Long sprintId) {
        return blockerRepository.findBySprintId(sprintId);
    }

    @PostMapping
    public Blocker raiseBlocker(@PathVariable Long sprintId, @RequestBody Blocker request) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        request.setSprint(sprint);
        Blocker savedBlocker = blockerRepository.save(request);

        // SYNC: Find the Task and mark it as blocked
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setIsBlocked(true);
        taskRepository.save(task);

        // TaskEvent event = new TaskEvent(
        //         savedBlocker.getTaskId().toString(),
        //         "BLOCKER_RAISED",
        //         "A blocker was raised on task: " + savedBlocker.getTaskTitle()
        // );
        // kafkaProducer.publishTaskEvent(event);

        // NOTIFICATION FIX: direct replacement for the block above.
        notificationService.createNotification(task, "BLOCKER_RAISED", "A blocker was raised on task: " + savedBlocker.getTaskTitle());

        return savedBlocker;
    }

    @PutMapping("/{blockerId}/resolve")
    public Blocker resolveBlocker(@PathVariable Long sprintId, @PathVariable Long blockerId) {
        Blocker blocker = blockerRepository.findById(blockerId)
                .orElseThrow(() -> new RuntimeException("Blocker not found"));

        blocker.setResolved(true);
        Blocker savedBlocker = blockerRepository.save(blocker);

        // SYNC: Find the Task and unblock it
        Task task = taskRepository.findById(savedBlocker.getTaskId()).orElse(null);
        if (task != null) {
            task.setIsBlocked(false);
            taskRepository.save(task);
        }

        // // OPTIONAL KAFKA POLISH: Tell the team the task is ready to be worked on again
        // TaskEvent event = new TaskEvent(
        //         savedBlocker.getTaskId().toString(),
        //         "BLOCKER_RESOLVED",
        //         "The blocker on task was resolved: " + savedBlocker.getTaskTitle()
        // );
        // kafkaProducer.publishTaskEvent(event);

        // NOTIFICATION FIX: direct replacement for the block above.
        notificationService.createNotification(task, "BLOCKER_RESOLVED", "The blocker on task was resolved: " + savedBlocker.getTaskTitle());

        return savedBlocker;
    }
}