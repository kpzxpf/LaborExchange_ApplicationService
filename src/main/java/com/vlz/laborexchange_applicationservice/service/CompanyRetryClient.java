package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.CompanyServiceClient;
import com.vlz.laborexchange_applicationservice.client.UserServiceClient;
import feign.FeignException;
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

    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${spring.retry.delay}"
            )
    )
    public String getCompanyName(Long id) {
        log.info("Attempting to fetch email for user id: {}", id);
        return companyServiceClient.getCompanyName(id);
    }

    @Recover
    public String recoverGetCompanyName(Exception e, Long userId) {
        log.error("Failed to fetch email for user id after retries: {}. Error: {}",
                userId, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service is currently unavailable", e);
    }
}
