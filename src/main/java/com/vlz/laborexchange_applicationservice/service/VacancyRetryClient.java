package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.VacancyClient;
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
public class VacancyRetryClient {

    private final VacancyClient vacancyClient;

    @CircuitBreaker(name = "vacancyService", fallbackMethod = "getVacancyTitleFallback")
    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getVacancyTitle(Long id) {
        log.info("Attempting to fetch vacancy title for id: {}", id);
        return vacancyClient.getById(id).getTitle();
    }

    public String getVacancyTitleFallback(Long id, Exception e) {
        log.warn("VacancyService circuit breaker open for title, vacancyId={}: {}", id, e.getMessage());
        return "Vacancy";
    }

    @Recover
    public String recoverVacancyTitle(Exception e, Long id) {
        log.error("Failed to fetch vacancy title after retries for id: {}. Error: {}", id, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Vacancy service is currently unavailable", e);
    }
}