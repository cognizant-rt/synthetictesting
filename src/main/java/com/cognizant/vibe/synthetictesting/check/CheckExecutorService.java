package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CheckResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static com.cognizant.vibe.synthetictesting.check.entity.CommandType.*;

/**
 * Executes a single CheckCommand. This service contains the logic
 * for performing the actual PING, GET, or TCP_PORT check.
 */
@Service
@RequiredArgsConstructor
public class CheckExecutorService {

    private static final Logger log = LoggerFactory.getLogger(CheckExecutorService.class);

    private final CheckResultRepository checkResultRepository;

    public void execute(CheckCommand command) {
        log.info("Executing check command ID: {} for target '{}' ({})",
                command.getId(), command.getApp().getName(), command.getApp().getTargetUrlOrIp());

        CheckResult checkResult = makeRequest(command);
        checkResultRepository.save(checkResult);
    }

    private CheckResult makeRequest(CheckCommand command) {
        return switch (command.getType()) {
            case GET -> executeGet(command);
            case PING -> executePing(command);
            case TCP_PORT -> executeTcpPortCheck(command);
            default -> throw new IllegalArgumentException("Unknown command type: " + command.getType());
        };
    }

    private CheckResult executeGet(CheckCommand command) {
        Instant startTime = Instant.now();
        // Placeholder for actual HTTP GET logic using HttpClient or RestTemplate
        log.info("-> Performing HTTP GET on {}", command.getApp().getTargetUrlOrIp());
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        return CheckResult.builder()
                .command(command)
                .timestamp(startTime)
                .success(true)
                .responseTimeMs(responseTime)
                .statusCode(200)
                .build();

    }

    private CheckResult executePing(CheckCommand command) {
        // Placeholder for actual ICMP ping logic
        log.info("-> Performing PING on {}", command.getApp().getTargetUrlOrIp());
        return null;
    }

    private CheckResult executeTcpPortCheck(CheckCommand command) {
        // Placeholder for actual TCP port check logic
        log.info("-> Performing TCP Port check on {} with params '{}'",
                command.getApp().getTargetUrlOrIp(), command.getParameters());
        return null;
    }
}