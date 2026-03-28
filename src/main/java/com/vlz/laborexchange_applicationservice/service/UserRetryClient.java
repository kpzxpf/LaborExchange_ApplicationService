package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.UserServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRetryClient {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "getEmailFallback")
    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getEmailByUserId(Long userId) {
        log.info("Attempting to fetch email for user id: {}", userId);
        return userServiceClient.getEmailById(userId);
    }

    public String getEmailFallback(Long userId, Exception e) {
        log.warn("UserService circuit breaker open for email, userId={}: {}", userId, e.getMessage());
        return null;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsernameFallback")
    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getUsernameByUserId(Long candidateId) {
        log.info("Attempting to fetch username for candidate id: {}", candidateId);
        return userServiceClient.getUsernameByUserId(candidateId);
    }

    public String getUsernameFallback(Long candidateId, Exception e) {
        log.warn("UserService circuit breaker open, returning fallback for userId={}: {}", candidateId, e.getMessage());
        return "Unknown User";
    }

    @Recover
    public String recoverEmailByUserId(Exception e, Long userId) {
        log.warn("Failed to fetch email for user id after retries: {}. Error: {}", userId, e.getMessage());
        return null;
    }
}