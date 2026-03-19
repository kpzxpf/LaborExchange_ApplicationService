package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.ResumeServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeRetryClient {
    private final ResumeServiceClient resumeServiceClient;

    @CircuitBreaker(name = "resumeService", fallbackMethod = "getResumeTitleFallback")
    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getResumeTitle(Long id) {
        log.info("Attempting to fetch title for resume id: {}", id);
        return resumeServiceClient.getResumeTitle(id);
    }

    public String getResumeTitleFallback(Long id, Exception e) {
        log.warn("ResumeService circuit breaker open for title, resumeId={}: {}", id, e.getMessage());
        return "Resume";
    }

    @Recover
    public String recoverGetResumeTitle(Exception e, Long resumeId) {
        log.error("Failed to fetch title for resume id after retries: {}. Error: {}",
                resumeId, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Resume service is currently unavailable", e);
    }
}
