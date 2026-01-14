package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.client.VacancyClient;
import com.vlz.laborexchange_applicationservice.dto.ApplicationEvent;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.producer.ApplicationNotificationProducer;
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
    private final ApplicationNotificationProducer applicationNotificationProducer;
    private final VacancyClient vacancyClient;

    @Transactional
    public Application createApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.NEW);
        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public Application getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByVacancy(Long vacancyId) {
        return applicationRepository.findByVacancyId(vacancyId).orElseThrow(()-> {
            log.error("Applications Not Found by VacancyId {}", vacancyId);
            return new EntityNotFoundException("Applications not found");
        });
    }

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByCandidate(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId).orElseThrow(()-> {
            log.error("Applications Not Found by CandidateId {}", candidateId);
            return new EntityNotFoundException("Applications not found");
        });
    }

    @Transactional
    public Application updateStatus(Long id, ApplicationStatusType statusType) {
        Application application = getById(id);
        application.setStatusFromType(statusType);
        Application savedApplication = applicationRepository.save(application);

        applicationNotificationProducer.send(ApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateId(savedApplication.getCandidateId())
                .statusCode(savedApplication.getStatus().getCode().name())
                .employerId(savedApplication.getEmployerId())
                .vacancyTitle(getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
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