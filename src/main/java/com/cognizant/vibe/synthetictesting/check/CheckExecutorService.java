package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import com.cognizant.vibe.synthetictesting.check.entity.CheckResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private final WebClient webClient;

    public void execute(CheckCommand command) {
        log.info("Executing check command ID: {} for target '{}' ({})",
                command.getId(), command.getApp().getName(), command.getApp().getTargetUrlOrIp());
        try {
            CheckResult result = makeRequest(command);
            checkResultRepository.save(result);
            log.info("Saved check result for command ID: {}. Success: {}", command.getId(), result.isSuccess());
        } catch (Exception e) {
            log.error("Unhandled exception during check execution for command ID {}: {}", command.getId(), e.getMessage(), e);
        }

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
        log.info("-> Performing HTTP GET on {}", command.getApp().getTargetUrlOrIp());
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .command(command)
                .timestamp(startTime);

        try {
            String targetUrl = command.getApp().getTargetUrlOrIp();
            webClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .toBodilessEntity() // We only care about the status, not the body
                    .doOnSuccess(response -> {
                        resultBuilder.success(response.getStatusCode().is2xxSuccessful())
                                .statusCode(response.getStatusCode().value());
                    })
                    .doOnError(error -> {
                        resultBuilder.success(false)
                                .errorMessage(error.getMessage());
                    })
                    .block(Duration.ofSeconds(10)); // Block for a result with a timeout

        } catch (Exception e) {
            log.error("Error executing GET for command ID {}: {}", command.getId(), e.getMessage());
            resultBuilder.success(false)
                    .errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        return resultBuilder.responseTimeMs(responseTime).build();
    }

    private CheckResult executePing(CheckCommand command) {
        log.info("-> Performing PING on {}", command.getApp().getTargetUrlOrIp());
        Instant startTime = Instant.now();
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .command(command)
                .timestamp(startTime);

        try {
            InetAddress.getByName(command.getApp().getTargetUrlOrIp());
            resultBuilder.success(true);
        } catch (Exception e) {
            resultBuilder.success(false).errorMessage("Unknown host " + command.getApp().getTargetUrlOrIp());
        }

        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        return resultBuilder.responseTimeMs(responseTime).build();
    }

    private CheckResult executeTcpPortCheck(CheckCommand command) {
        log.info("-> Performing TCP Port check on {} with params '{}'",
                command.getApp().getTargetUrlOrIp(), command.getParameters());
        Instant startTime = Instant.now();
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .command(command)
                .timestamp(startTime);

        try {
            String host = command.getApp().getTargetUrlOrIp();
            int port = Integer.parseInt(command.getParameters());
            int timeoutMs = 5000; // 5-second timeout
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                resultBuilder.success(true);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid port '{}' for TCP check on command ID {}", command.getParameters(), command.getId());
            resultBuilder.success(false).errorMessage("Invalid port number: " + command.getParameters());
        } catch (Exception e) {
            resultBuilder.success(false).errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        return resultBuilder.responseTimeMs(responseTime).build();
    }
}