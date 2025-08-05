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
        log.info("-> Performing PING on {} with params '{}'",
                command.getApp().getTargetUrlOrIp(), command.getParameters());
        Instant startTime = Instant.now();
        CheckResult.CheckResultBuilder resultBuilder = CheckResult.builder()
                .command(command)
                .timestamp(startTime);

        try {
            String host = command.getApp().getTargetUrlOrIp();
            int timeoutMs = 5000; // Default 5-second timeout

            // Allow overriding timeout via parameters, similar to the TCP check
            if (command.getParameters() != null && !command.getParameters().isBlank()) {
                try {
                    timeoutMs = Integer.parseInt(command.getParameters());
                } catch (NumberFormatException e) {
                    log.warn("Invalid timeout parameter for PING check on command ID {}: '{}'. Using default {}ms.",
                            command.getId(), command.getParameters(), timeoutMs);
                }
            }

            InetAddress inetAddress = InetAddress.getByName(host);
            boolean isReachable = inetAddress.isReachable(timeoutMs);

            resultBuilder.success(isReachable);
            if (!isReachable) {
                resultBuilder.errorMessage("Host is not reachable (timeout: " + timeoutMs + "ms).");
            }
        } catch (java.net.UnknownHostException e) {
            log.warn("Unknown host for PING check on command ID {}: {}", command.getId(), command.getApp().getTargetUrlOrIp());
            resultBuilder.success(false).errorMessage("Unknown host: " + command.getApp().getTargetUrlOrIp());
        } catch (java.io.IOException e) {
            log.error("IO error during PING check for command ID {}: {}", command.getId(), e.getMessage());
            resultBuilder.success(false).errorMessage("IO Error during ping: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during PING check for command ID {}: {}", command.getId(), e.getMessage(), e);
            resultBuilder.success(false).errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
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
            String params = command.getParameters();
            if (params == null || params.isBlank()) {
                throw new IllegalArgumentException("Port must be specified in parameters.");
            }

            String[] paramParts = params.split(":", 2);
            int port = Integer.parseInt(paramParts[0]);
            int timeoutMs = 5000; // Default 5-second timeout

            if (paramParts.length > 1) {
                timeoutMs = Integer.parseInt(paramParts[1]);
            }

            String host = command.getApp().getTargetUrlOrIp();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                resultBuilder.success(true);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters '{}' for TCP check on command ID {}: {}",
                    command.getParameters(), command.getId(), e.getMessage());
            resultBuilder.success(false).errorMessage("Invalid parameters: " + command.getParameters());
        } catch (java.net.UnknownHostException e) {
            log.warn("Unknown host for TCPs check on command ID {}: {}", command.getId(), command.getApp().getTargetUrlOrIp());
            resultBuilder.success(false).errorMessage("Unknown host: " + command.getApp().getTargetUrlOrIp());
        } catch (java.net.SocketTimeoutException e) {
            log.warn("TCP check timed out for command ID {}: {}", command.getId(), e.getMessage());
            resultBuilder.success(false).errorMessage("Connection timed out.");
        } catch (java.io.IOException e) {
            log.error("IO error during TCP check for command ID {}: {}", command.getId(), e.getMessage());
            resultBuilder.success(false).errorMessage("IO Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during TCP check for command ID {}: {}", command.getId(), e.getMessage(), e);
            resultBuilder.success(false).errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        return resultBuilder.responseTimeMs(responseTime).build();
    }
}