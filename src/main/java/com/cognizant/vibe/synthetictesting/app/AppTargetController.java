package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.check.dto.CheckCommandResultsDto;
import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CreateCheckCommandRequest;
import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import com.cognizant.vibe.synthetictesting.app.entity.CreateAppTargetRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/targets")
@RequiredArgsConstructor
public class AppTargetController {

    private final AppTargetService appTargetService;

    @PostMapping
    public ResponseEntity<AppTarget> createAppTarget(@Valid @RequestBody CreateAppTargetRequest request) {
        AppTarget createdTarget = appTargetService.createAppTarget(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTarget.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdTarget);
    }

    @GetMapping
    public ResponseEntity<List<AppTarget>> getAllTargets() {
        List<AppTarget> targets = appTargetService.getAllTargets();
        return ResponseEntity.ok(targets);
    }

    @PostMapping("/{targetId}/checks")
    public ResponseEntity<CheckCommand> addCheckCommand(
            @PathVariable Long targetId,
            @Valid @RequestBody CreateCheckCommandRequest request) {

        CheckCommand createdCommand = appTargetService.addCheckCommandToTarget(targetId, request);

        // Build the location URI of the new sub-resource.
        // e.g., /api/v1/targets/1/checks/101
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCommand.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdCommand);
    }

    @GetMapping("/{targetId}/checks")
    public ResponseEntity<List<CheckCommand>> getCheckCommandsForTarget(@PathVariable Long targetId) {
        List<CheckCommand> commands = appTargetService.getCheckCommandsForTarget(targetId);
        return ResponseEntity.ok(commands);
    }

    @DeleteMapping("/{targetId}/checks/{checkId}")
    public ResponseEntity<Void> deleteCheckCommand(@PathVariable Long targetId, @PathVariable Long checkId) {
        appTargetService.deleteCheckCommand(targetId, checkId);
        return ResponseEntity.noContent().build(); // HTTP 204 No Content is standard for successful DELETE
    }

    @GetMapping("/{targetId}/results")
    public ResponseEntity<List<CheckCommandResultsDto>> getCheckResultsForTarget(@PathVariable Long targetId) {
        List<CheckCommandResultsDto> results = appTargetService.getCheckResultsForTarget(targetId);
        return ResponseEntity.ok(results);
    }
}