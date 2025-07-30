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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the scheduling of all synthetic checks at application startup.
 */
@Service
@RequiredArgsConstructor
public class CheckSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CheckSchedulerService.class);

    private final AppTargetRepository appTargetRepository;
    private final CheckCommandRepository checkCommandRepository;
    private final CheckExecutorService checkExecutorService;
    private final ScheduledExecutorService checkSchedulerExecutor;

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

    private void scheduleSingleCommand(CheckCommand command) {
        // Create a runnable task that will call the executor service.
        Runnable task = () -> checkExecutorService.execute(command);

        long interval = command.getIntervalSeconds();
        if (interval < 5) {
            log.warn("Check command ID {} has an interval of {}s, which is below the recommended minimum. Skipping.", command.getId(), interval);
            return;
        }

        // Schedule the task to run at a fixed rate.
        checkSchedulerExecutor.scheduleAtFixedRate(task, 5, interval, TimeUnit.SECONDS);

        log.info("Scheduled check ID: {} for target '{}' to run every {} seconds.",
                command.getId(), command.getApp().getName(), interval);
    }
}