package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.app.AppTargetRepository;
import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the scheduling of all synthetic checks.
 */
@Service
@RequiredArgsConstructor
public class CheckSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CheckSchedulerService.class);

    private final AppTargetRepository appTargetRepository;
    private final CheckCommandRepository checkCommandRepository;
    private final CheckExecutorService checkExecutorService;
    private final ScheduledExecutorService checkSchedulerExecutor;

    // A map to hold references to scheduled tasks, allowing them to be cancelled later.
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * This method is executed by Spring after the application context is loaded.
     * It fetches all enabled targets and their commands, then schedules them for execution.
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void scheduleChecksAtStartup() {
        log.info("Starting to schedule synthetic checks...");

        List<AppTarget> enabledTargets = appTargetRepository.findByEnabled(true);
        log.info("Found {} enabled application targets to monitor.", enabledTargets.size());

        int scheduledCount = 0;
        for (AppTarget target : enabledTargets) {
            // Use the new method to fetch commands with their parent AppTarget to avoid LazyInitializationException
            List<CheckCommand> commands = checkCommandRepository.findByAppIdWithAppTarget(target.getId());
            if (commands.isEmpty()) {
                log.warn("Target '{}' is enabled but has no check commands.", target.getName());
                continue;
            }

            for (CheckCommand command : commands) {
                scheduleSingleCommand(command);
                scheduledCount++;
            }
        }
        log.info("Successfully scheduled {} checks.", scheduledCount);
    }

    /**
     * Schedules a single check command to run at its configured interval.
     * This can be called at startup or when a new command is created at runtime.
     *
     * @param command The CheckCommand to schedule. The associated AppTarget should be fully initialized.
     */
    public void scheduleSingleCommand(CheckCommand command) {
        if (scheduledTasks.containsKey(command.getId())) {
            log.warn("Check command ID {} is already scheduled. Skipping.", command.getId());
            return;
        }
        // Create a runnable task that will call the executor service.
        Runnable task = () -> checkExecutorService.execute(command);

        long interval = command.getIntervalSeconds();
        if (interval < 5) {
            log.warn("Check command ID {} has an interval of {}s, which is below the recommended minimum. Skipping.", command.getId(), interval);
            return;
        }

        // Schedule the task to run at a fixed rate with an initial delay of 5 seconds.
        ScheduledFuture<?> future = checkSchedulerExecutor.scheduleAtFixedRate(task, 5, interval, TimeUnit.SECONDS);
        scheduledTasks.put(command.getId(), future);

        log.info("Scheduled check ID: {} for target '{}' to run every {} seconds.",
                command.getId(), command.getApp().getName(), interval);
    }

    /**
     * Unschedules and removes a running check command.
     *
     * @param checkId The ID of the command to unschedule.
     */
    public void unscheduleSingleCommand(Long checkId) {
        ScheduledFuture<?> future = scheduledTasks.get(checkId);
        if (future != null) {
            // Cancel the task. The 'false' argument means do not interrupt the task if it's currently running.
            future.cancel(false);
            scheduledTasks.remove(checkId);
            log.info("Unscheduled check command ID: {}", checkId);
        } else {
            log.warn("Could not unschedule check command ID: {}. It was not found in the scheduler.", checkId);
        }
    }
}