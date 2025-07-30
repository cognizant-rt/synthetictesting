package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.CheckCommandRepository;
import com.cognizant.vibe.synthetictesting.check.entity.CreateCheckCommandRequest;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import com.cognizant.vibe.synthetictesting.app.entity.CreateAppTargetRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppTargetService {

    private final AppTargetRepository appTargetRepository;
    private final CheckCommandRepository checkCommandRepository;

    public AppTargetService(AppTargetRepository appTargetRepository, CheckCommandRepository checkCommandRepository) {
        this.appTargetRepository = appTargetRepository;
        this.checkCommandRepository = checkCommandRepository;
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
        return checkCommandRepository.save(newCommand);
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