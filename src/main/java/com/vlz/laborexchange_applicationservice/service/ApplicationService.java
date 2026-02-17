package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.dto.*;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.exception.DuplicateApplicationException;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
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
import java.util.concurrent.CompletableFuture;
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
    private final ApplicationMapper applicationMapper;
    private final ResumeRetryClient resumeRetryClient;
    private final CompanyRetryClient companyRetryClient;

    @Transactional
    public Application createApplication(ApplicationRequestDto applicationDto) {
        Application application = Application.builder()
                .vacancyId(applicationDto.getVacancyId())
                .candidateId(applicationDto.getCandidateId())
                .employerId(applicationDto.getEmployerId())
                .resumeId(applicationDto.getResumeId())
                .build();

        application.setStatusFromType(ApplicationStatusType.NEW);

        existsByVacancyIdAndCandidateIdAndResumeId(
                application.getVacancyId(), application.getCandidateId(), application.getResumeId());

        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> newApplicationNotificationProducer.send(NewApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()));

        return savedApplication;
    }

    @Transactional
    public Application rejectApplication(ApplicationRequestDto applicationDto) {
        Application application = getById(applicationDto.getId());
        application.setStatusFromType(ApplicationStatusType.REJECTED);


        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> rejectedApplicationNotificationProducer.send(RejectedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()));

        return savedApplication;
    }

    @Transactional
    public Application withdrawnApplication(ApplicationRequestDto applicationDto) {
        Application application = getById(applicationDto.getId());
        application.setStatusFromType(ApplicationStatusType.WITHDRAWN);

        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> withdrawnApplicationProducer.send(WithdrawnApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()));

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
    public List<ApplicationResponseDto> getApplicationsByVacancy(Long vacancyId) {
        List<Application> applications = applicationRepository.findByVacancyId(vacancyId)
                .orElseThrow(() -> {
                    log.error("Applications Not Found by VacancyId {}", vacancyId);
                    return new EntityNotFoundException("Applications not found");
                });

        return applications.stream()
                .map(applicationMapper::toDto)
                .map(this::enrichApplicationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByCandidate(Long candidateId) {
        List<Application> applications = applicationRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> {
                    log.error("Applications Not Found by CandidateId {}", candidateId);
                    return new EntityNotFoundException("Applications not found");
                });

        return applications.stream()
                .map(applicationMapper::toDto)
                .map(this::enrichApplicationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatusType statusType) {
        List<Application> applications = applicationRepository.findByStatus_Code(statusType)
                .orElseThrow(() -> {
                    log.error("Applications not found for status {}", statusType);
                    return new EntityNotFoundException("No applications found for status: " + statusType);
                });

        return applications.stream()
                .map(applicationMapper::toDto)
                .map(this::enrichApplicationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByEmployer(Long employerId) {
        log.info("Fetching applications for employer: {}", employerId);

        List<Application> applications = applicationRepository.findByEmployerId(employerId)
                .orElseThrow(() -> {
                    log.error("Applications Not Found by EmployerId {}", employerId);
                    return new EntityNotFoundException("Applications not found for employer: " + employerId);
                });

        return applications.stream()
                .map(applicationMapper::toDto)
                .map(this::enrichApplicationDto)
                .collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public ApplicationStatisticsDto getEmployerStatistics(Long employerId) {
        log.info("Fetching statistics for employer: {}", employerId);

        Long total = applicationRepository.countByEmployerId(employerId);

        List<Object[]> grouped = applicationRepository.countByEmployerIdAndStatusGrouped(employerId);
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
        Long active = applicationRepository.countByEmployerIdAndStatusIn(employerId, activeStatuses);

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

    private ApplicationResponseDto enrichApplicationDto(ApplicationResponseDto dto) {
        try {
            String vacancyTitle = vacancyRetryClient.getVacancyTitle(dto.getVacancyId());
            String companyName = companyRetryClient.getCompanyName(dto.getVacancyId());

            String candidateName = userRetryClient.getUsernameByUserId(dto.getCandidateId());
            String candidateEmail = userRetryClient.getEmailByUserId(dto.getCandidateId());

            String resumeTitle = resumeRetryClient.getResumeTitle(dto.getResumeId());

            dto.setVacancyTitle(vacancyTitle);
            dto.setCompanyName(companyName);
            dto.setCandidateName(candidateName);
            dto.setCandidateEmail(candidateEmail);
            dto.setResumeTitle(resumeTitle);
        } catch (Exception e) {
            log.warn("Failed to enrich application DTO: {}", e.getMessage());
        }

        return dto;
    }
}