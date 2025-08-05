package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.app.dto.AppTargetDto;
import com.cognizant.vibe.synthetictesting.check.dto.CheckCommandResultsDto;
import com.cognizant.vibe.synthetictesting.check.dto.CheckCommandDto;
import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CreateCheckCommandRequest;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import com.cognizant.vibe.synthetictesting.app.entity.CreateAppTargetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/targets")
@Tag(name = "Application Targets", description = "APIs for managing application targets and their associated checks.")
@RequiredArgsConstructor
public class AppTargetController {

    private final AppTargetService appTargetService;

    @PostMapping
    @Operation(summary = "Create a new application target", description = "Creates a new target to be monitored.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Target created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppTargetDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    public ResponseEntity<AppTargetDto> createAppTarget(@Valid @RequestBody CreateAppTargetRequest request) {
        AppTarget createdTarget = appTargetService.createAppTarget(request);
        URI location = buildLocationUri(createdTarget.getId());
        return ResponseEntity.created(location).body(toDto(createdTarget));
    }

    @GetMapping
    @Operation(summary = "Get all application targets", description = "Retrieves a list of all configured application targets.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of targets")
    public ResponseEntity<List<AppTargetDto>> getAllTargets() {
        List<AppTarget> targets = appTargetService.getAllTargets();
        List<AppTargetDto> targetDtos = targets.stream().map(this::toDto).toList();
        return ResponseEntity.ok(targetDtos);
    }

    @PostMapping("/{targetId}/checks")
    @Operation(summary = "Add a check command to a target", description = "Adds a new check command (e.g., HTTP check, Ping check) to an existing target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Check command added successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckCommandDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content)
    })
    public ResponseEntity<CheckCommandDto> addCheckCommand(
            @PathVariable Long targetId,
            @Valid @RequestBody CreateCheckCommandRequest request) {

        CheckCommand createdCommand = appTargetService.addCheckCommandToTarget(targetId, request);
        URI location = buildLocationUri(createdCommand.getId());
        return ResponseEntity.created(location).body(toDto(createdCommand));
    }

    @GetMapping("/{targetId}/checks")
    @Operation(summary = "Get all check commands for a target", description = "Retrieves all check commands associated with a specific target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of check commands"),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content)
    })
    public ResponseEntity<List<CheckCommandDto>> getCheckCommandsForTarget(@PathVariable Long targetId) {
        List<CheckCommand> commands = appTargetService.getCheckCommandsForTarget(targetId);
        List<CheckCommandDto> commandDtos = commands.stream().map(this::toDto).toList();
        return ResponseEntity.ok(commandDtos);
    }

    @DeleteMapping("/{targetId}/checks/{checkId}")
    @Operation(summary = "Delete a check command", description = "Deletes a specific check command from a target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Check command deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Target or Check command not found", content = @Content)
    })
    public ResponseEntity<Void> deleteCheckCommand(@PathVariable Long targetId, @PathVariable Long checkId) {
        appTargetService.deleteCheckCommand(targetId, checkId);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content is standard for successful DELETE
    }

    @GetMapping("/{targetId}/results")
    @Operation(summary = "Get check results for a target", description = "Retrieves the latest check results for all checks associated with a specific target.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved check results"),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content)
    })
    public ResponseEntity<List<CheckCommandResultsDto>> getCheckResultsForTarget(@PathVariable Long targetId) {
        List<CheckCommandResultsDto> results = appTargetService.getCheckResultsForTarget(targetId);
        return ResponseEntity.ok(results);
    }

    // --- Private Helper Methods ---

    private URI buildLocationUri(Object resourceId) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resourceId)
                .toUri();
    }

    private AppTargetDto toDto(AppTarget entity) {
        return AppTargetDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .targetUrlOrIp(entity.getTargetUrlOrIp())
                .type(entity.getType())
                .enabled(entity.isEnabled())
                .build();
    }

    private CheckCommandDto toDto(CheckCommand entity) {
        return CheckCommandDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .parameters(entity.getParameters())
                .intervalSeconds(entity.getIntervalSeconds())
                .build();
    }
}