package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.check.CheckCommandRepository;
import com.cognizant.vibe.synthetictesting.check.CheckSchedulerService;
import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CreateCheckCommandRequest;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import com.cognizant.vibe.synthetictesting.app.entity.CreateAppTargetRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppTargetService {

    private static final Logger log = LoggerFactory.getLogger(AppTargetService.class);

    private final AppTargetRepository appTargetRepository;
    private final CheckCommandRepository checkCommandRepository;
    private final CheckSchedulerService checkSchedulerService;

    public AppTargetService(AppTargetRepository appTargetRepository,
                            CheckCommandRepository checkCommandRepository,
                            CheckSchedulerService checkSchedulerService) {
        this.appTargetRepository = appTargetRepository;
        this.checkCommandRepository = checkCommandRepository;
        this.checkSchedulerService = checkSchedulerService;
    }

    @Transactional
    public AppTarget createAppTarget(CreateAppTargetRequest request) {
        AppTarget newTarget = AppTarget.builder()
                .name(request.name())
                .targetUrlOrIp(request.targetUrlOrIp())
                .type(request.type())
                .enabled(request.enabled())
                .build();

        return appTargetRepository.save(newTarget);
    }

    @Transactional(readOnly = true)
    public List<AppTarget> getAllTargets() {
        return appTargetRepository.findAll();
    }

    @Transactional
    public CheckCommand addCheckCommandToTarget(Long targetId, CreateCheckCommandRequest request) {
        // 1. Find the parent AppTarget, or throw an exception if it doesn't exist.
        AppTarget target = appTargetRepository.findById(targetId)
                .orElseThrow(() -> new EntityNotFoundException("AppTarget not found with id: " + targetId));

        // 2. Build the new CheckCommand entity.
        CheckCommand newCommand = CheckCommand.builder()
                .app(target) // Link it to the parent AppTarget.
                .type(request.type())
                .parameters(request.parameters())
                .intervalSeconds(request.intervalSeconds())
                .build();

        // 3. Save the new command to the database.
        CheckCommand savedCommand = checkCommandRepository.save(newCommand);

        // 4. If the parent target is enabled, schedule the new command immediately.
        if (target.isEnabled()) {
            checkSchedulerService.scheduleSingleCommand(savedCommand);
        } else {
            log.warn("Check command ID {} was created for a disabled target '{}'. It will not be scheduled.",
                    savedCommand.getId(), target.getName());
        }

        return savedCommand;
    }

    /**
     * Retrieves all CheckCommands for a specific AppTarget.
     * The transaction is read-only for better performance.
     *
     * @param targetId The ID of the parent AppTarget.
     * @return A list of CheckCommand entities.
     * @throws EntityNotFoundException if no AppTarget with the given ID is found.
     */
    @Transactional(readOnly = true)
    public List<CheckCommand> getCheckCommandsForTarget(Long targetId) {
        // First, ensure the parent AppTarget exists to provide a clear 404 Not Found response from the controller.
        if (!appTargetRepository.existsById(targetId)) {
            throw new EntityNotFoundException("AppTarget not found with id: " + targetId);
        }
        // Then, fetch the associated check commands using our new repository method.
        return checkCommandRepository.findByAppId(targetId);
    }
}