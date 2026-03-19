package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.CompanyServiceClient;
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
public class CompanyRetryClient {
    private final CompanyServiceClient companyServiceClient;

    @CircuitBreaker(name = "companyService", fallbackMethod = "getCompanyNameFallback")
    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getCompanyName(Long id) {
        log.info("Attempting to fetch company name for id: {}", id);
        return companyServiceClient.getCompanyName(id);
    }

    public String getCompanyNameFallback(Long id, Exception e) {
        log.warn("CompanyService circuit breaker open for company name, companyId={}: {}", id, e.getMessage());
        return "Company";
    }

    @Recover
    public String recoverGetCompanyName(Exception e, Long vacancyId) {
        log.error("Failed to fetch company name after retries for vacancyId: {}. Error: {}",
                vacancyId, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Company service is currently unavailable", e);
    }
}
