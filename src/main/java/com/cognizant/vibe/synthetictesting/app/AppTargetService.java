package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.check.CheckCommandRepository;
import com.cognizant.vibe.synthetictesting.check.CheckResultRepository;
import com.cognizant.vibe.synthetictesting.check.CheckSchedulerService;
import com.cognizant.vibe.synthetictesting.check.dto.CheckCommandResultsDto;
import com.cognizant.vibe.synthetictesting.check.dto.CheckResultDto;
import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CheckResult;
import com.cognizant.vibe.synthetictesting.check.entity.CreateCheckCommandRequest;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import com.cognizant.vibe.synthetictesting.app.entity.CreateAppTargetRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppTargetService {

    private static final Logger log = LoggerFactory.getLogger(AppTargetService.class);

    private final AppTargetRepository appTargetRepository;
    private final CheckCommandRepository checkCommandRepository;
    private final CheckSchedulerService checkSchedulerService;
    private final CheckResultRepository checkResultRepository;

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
     * Deletes a check command by its ID after unscheduling it.
     *
     * @param targetId The ID of the parent AppTarget.
     * @param checkId The ID of the CheckCommand to delete.
     * @throws EntityNotFoundException if the check command is not found.
     * @throws IllegalArgumentException if the check command does not belong to the specified target.
     */
    @Transactional
    public void deleteCheckCommand(Long targetId, Long checkId) {
        // 1. Find the command and verify it belongs to the specified target to ensure data integrity.
        CheckCommand command = checkCommandRepository.findById(checkId)
                .orElseThrow(() -> new EntityNotFoundException("CheckCommand not found with id: " + checkId));

        if (!command.getApp().getId().equals(targetId)) {
            throw new IllegalArgumentException("CheckCommand with id " + checkId + " does not belong to AppTarget with id " + targetId);
        }

        // 2. Unschedule the command from the running scheduler to stop its execution.
        checkSchedulerService.unscheduleSingleCommand(checkId);

        // 3. Delete the command from the database.
        checkCommandRepository.delete(command);
        log.info("Successfully deleted and unscheduled check command ID: {}", checkId);
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
        // Use the query that eagerly fetches the AppTarget to prevent serialization issues.
        // This is more efficient as it's one query in the happy path.
        List<CheckCommand> commands = checkCommandRepository.findByAppIdWithAppTarget(targetId);

        // If the commands list is empty, we must check if it's because the target
        // doesn't exist (should be 404), or if it just has no commands (should be 200 with an empty list).
        if (commands.isEmpty() && !appTargetRepository.existsById(targetId)) {
            throw new EntityNotFoundException("AppTarget not found with id: " + targetId);
        }

        return commands;
    }

    /**
     * Retrieves all check results for a given AppTarget, grouped by their respective CheckCommand.
     *
     * @param targetId The ID of the parent AppTarget.
     * @return A list of DTOs, each containing a command's info and its list of results.
     * @throws EntityNotFoundException if no AppTarget with the given ID is found.
     */
    @Transactional(readOnly = true)
    public List<CheckCommandResultsDto> getCheckResultsForTarget(Long targetId) {
        // For production systems with many results, consider adding a time window filter (e.g., last 24 hours)
        // and/or pagination to this query to limit the data returned.
        List<CheckResult> results = checkResultRepository.findResultsForTarget(targetId);

        // Handle the case where the target does not exist.
        if (results.isEmpty() && !appTargetRepository.existsById(targetId)) {
            throw new EntityNotFoundException("AppTarget not found with id: " + targetId);
        }

        // Group the flat list of results by their parent command.
        Map<CheckCommand, List<CheckResult>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(result -> result.getCommand()));

        // Map the grouped data to our DTO structure.
        return groupedResults.entrySet().stream()
                .map(entry -> {
                    CheckCommand command = entry.getKey();
                    List<CheckResultDto> resultDtos = entry.getValue().stream()
                            .map(r -> new CheckResultDto(r.getId(), r.getTimestamp(), r.isSuccess(), r.getResponseTimeMs(), r.getStatusCode(), r.getErrorMessage()))
                            .collect(Collectors.toList());
                    return new CheckCommandResultsDto(command.getId(), command.getType(), command.getParameters(), resultDtos);
                })
                .sorted(Comparator.comparing(CheckCommandResultsDto::commandId)) // Sort for a consistent API response order.
                .collect(Collectors.toList());
    }
}