package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.dto.ApplicationStatisticsDto;
import com.vlz.laborexchange_applicationservice.dto.NewApplicationEvent;
import com.vlz.laborexchange_applicationservice.dto.RejectedApplicationEvent;
import com.vlz.laborexchange_applicationservice.dto.WithdrawnApplicationEvent;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.exception.DuplicateApplicationException;
import com.vlz.laborexchange_applicationservice.producer.NewApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.RejectedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.WithdrawnApplicationProducer;
import com.vlz.laborexchange_applicationservice.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final NewApplicationNotificationProducer newApplicationNotificationProducer;
    private final RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer;
    private final WithdrawnApplicationProducer withdrawnApplicationProducer;
    private final VacancyRetryClient vacancyRetryClient;
    private final UserRetryClient userRetryClient;

    @Transactional
    public Application createApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.NEW);

        existsByVacancyIdAndCandidateIdAndResumeId(
                application.getVacancyId(), application.getCandidateId(), application.getResumeId());

        Application savedApplication = applicationRepository.save(application);

        newApplicationNotificationProducer.send(NewApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }

    @Transactional
    public Application rejectApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.REJECTED);

        Application savedApplication = applicationRepository.save(application);

        rejectedApplicationNotificationProducer.send(RejectedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }

    @Transactional
    public Application withdrawnApplication(Application application) {
        application.setStatusFromType(ApplicationStatusType.WITHDRAWN);

        Application savedApplication = applicationRepository.save(application);

        withdrawnApplicationProducer.send(WithdrawnApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build());

        return savedApplication;
    }

    @Transactional(readOnly = true)
    public Application getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Application with id {} not found", id);
                    return new EntityNotFoundException("Application with id " + id + " not found");
                });
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

    @Transactional(readOnly = true)
    public List<Application> getApplicationsByStatus(ApplicationStatusType statusType) {
        return applicationRepository.findByStatus_Code(statusType)
                .orElseThrow(() -> {
                    log.error("Applications not found for status {}", statusType);
                    return new EntityNotFoundException("No applications found for status: " + statusType);
                });
    }

    @Transactional(readOnly = true)
    public ApplicationStatisticsDto getStatistics() {
        Long total = applicationRepository.count();

        List<Object[]> grouped = applicationRepository.countByStatusGrouped();
        Map<String, Long> byStatus = grouped.stream()
                .collect(Collectors.toMap(
                        arr -> ((ApplicationStatusType) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));

        List<ApplicationStatusType> activeStatuses = List.of(
                ApplicationStatusType.NEW,
                ApplicationStatusType.WITHDRAWN,
                ApplicationStatusType.REJECTED
        );
        Long active = applicationRepository.countByStatusIn(activeStatuses);

        Long withdrawn = byStatus.getOrDefault(ApplicationStatusType.WITHDRAWN.name(), 0L);
        Double withdrawalRate = total > 0 ? (withdrawn * 100.0 / total) : 0.0;

        return ApplicationStatisticsDto.builder()
                .totalApplications(total)
                .applicationsByStatus(byStatus)
                .activeApplications(active)
                .withdrawalRate(withdrawalRate)
                .build();
    }

    private void existsByVacancyIdAndCandidateIdAndResumeId(
            Long vacancyId, Long candidateId, Long resumeId) {
        if (applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(vacancyId, candidateId, resumeId)) {
            log.warn("Duplicate application detected for vacancy={}, candidate={}",
                    vacancyId, candidateId);

            throw new DuplicateApplicationException(
                    "Application for this vacancy already exists");
        }
    }
}