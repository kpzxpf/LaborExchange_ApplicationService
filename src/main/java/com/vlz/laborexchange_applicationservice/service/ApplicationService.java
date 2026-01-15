package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.UserServiceClient;
import com.vlz.laborexchange_applicationservice.client.VacancyClient;
import com.vlz.laborexchange_applicationservice.dto.NewApplicationEvent;
import com.vlz.laborexchange_applicationservice.dto.RejectedApplicationEvent;
import com.vlz.laborexchange_applicationservice.dto.WithdrawnApplicationEvent;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.producer.NewApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.RejectedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.WithdrawnApplicationProducer;
import com.vlz.laborexchange_applicationservice.repository.ApplicationRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final NewApplicationNotificationProducer newApplicationNotificationProducer;
    private final RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer;
    private final WithdrawnApplicationProducer withdrawnApplicationProducer;
    private final VacancyClient vacancyClient;
    private final UserServiceClient userServiceClient;

    @Transactional
    public Application createApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.NEW);

        Application savedApplication = applicationRepository.save(application);

        newApplicationNotificationProducer.send(NewApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }

    @Transactional
    public Application rejectApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.REJECTED);

        Application savedApplication = applicationRepository.save(application);

        rejectedApplicationNotificationProducer.send(RejectedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateEmail(getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }

    @Transactional
    public Application withdrawnApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.WITHDRAWN);

        Application savedApplication = applicationRepository.save(application);

        withdrawnApplicationProducer.send(WithdrawnApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }



    @Transactional(readOnly = true)
    public Application getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByVacancy(Long vacancyId) {
        return applicationRepository.findByVacancyId(vacancyId).orElseThrow(() -> {
            log.error("Applications Not Found by VacancyId {}", vacancyId);
            return new EntityNotFoundException("Applications not found");
        });
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId).orElseThrow(() -> {
            log.error("Applications Not Found by CandidateId {}", candidateId);
            return new EntityNotFoundException("Applications not found");
        });
    }

    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String getEmailByUserId(Long userId) {
        return userServiceClient.getEmailById(userId);
    }

    @Recover
    public String recoverEmailByUserId(FeignException e, Long userId) {
        log.error("Failed to fetch email for user id: {}. Status: {}",
                userId, e.status(), e);
        return null;
    }

    @Retryable(
            retryFor = {FeignException.class, IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String getVacancyTitle(Long id) {
        log.info("Attempting to fetch vacancy title for id: {}", id);
        return vacancyClient.getById(id).getTitle();
    }

    @Recover
    public String recoverVacancyTitle(Exception e, Long id) {
        log.error("Failed to fetch vacancy title after retries for id: {}. Error: {}", id, e.getMessage());
        return "Position not specified";
    }
}