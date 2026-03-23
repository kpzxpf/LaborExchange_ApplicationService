package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.dto.*;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatus;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.exception.DuplicateApplicationException;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
import com.vlz.laborexchange_applicationservice.producer.AcceptedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.NewApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.RejectedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.WithdrawnApplicationProducer;
import com.vlz.laborexchange_applicationservice.repository.ApplicationRepository;
import com.vlz.laborexchange_applicationservice.repository.ApplicationStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusRepository statusRepository;
    private final NewApplicationNotificationProducer newApplicationNotificationProducer;
    private final RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer;
    private final WithdrawnApplicationProducer withdrawnApplicationProducer;
    private final AcceptedApplicationNotificationProducer acceptedApplicationNotificationProducer;
    private final VacancyRetryClient vacancyRetryClient;
    private final UserRetryClient userRetryClient;
    private final ApplicationMapper applicationMapper;
    private final ResumeRetryClient resumeRetryClient;
    private final CompanyRetryClient companyRetryClient;
    private final ExecutorService notificationExecutor;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationStatusRepository statusRepository,
            NewApplicationNotificationProducer newApplicationNotificationProducer,
            RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer,
            WithdrawnApplicationProducer withdrawnApplicationProducer,
            AcceptedApplicationNotificationProducer acceptedApplicationNotificationProducer,
            VacancyRetryClient vacancyRetryClient,
            UserRetryClient userRetryClient,
            ApplicationMapper applicationMapper,
            ResumeRetryClient resumeRetryClient,
            CompanyRetryClient companyRetryClient,
            @Qualifier("notificationExecutor") ExecutorService notificationExecutor) {
        this.applicationRepository = applicationRepository;
        this.statusRepository = statusRepository;
        this.newApplicationNotificationProducer = newApplicationNotificationProducer;
        this.rejectedApplicationNotificationProducer = rejectedApplicationNotificationProducer;
        this.withdrawnApplicationProducer = withdrawnApplicationProducer;
        this.acceptedApplicationNotificationProducer = acceptedApplicationNotificationProducer;
        this.vacancyRetryClient = vacancyRetryClient;
        this.userRetryClient = userRetryClient;
        this.applicationMapper = applicationMapper;
        this.resumeRetryClient = resumeRetryClient;
        this.companyRetryClient = companyRetryClient;
        this.notificationExecutor = notificationExecutor;
    }

    private ApplicationStatus resolveStatus(ApplicationStatusType type) {
        return statusRepository.findByCode(type)
                .orElseThrow(() -> new EntityNotFoundException("ApplicationStatus not found for type: " + type));
    }

    @Transactional
    public Application createApplication(ApplicationRequestDto applicationDto) {
        existsByVacancyIdAndCandidateIdAndResumeId(
                applicationDto.getVacancyId(), applicationDto.getCandidateId(), applicationDto.getResumeId());

        Application application = Application.builder()
                .vacancyId(applicationDto.getVacancyId())
                .candidateId(applicationDto.getCandidateId())
                .employerId(applicationDto.getEmployerId())
                .resumeId(applicationDto.getResumeId())
                .coverLetter(applicationDto.getCoverLetter())
                .build();

        application.setStatus(resolveStatus(ApplicationStatusType.NEW));

        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> newApplicationNotificationProducer.send(NewApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerId(savedApplication.getEmployerId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);

        return savedApplication;
    }

    @Transactional
    public Application rejectApplication(ApplicationRequestDto applicationDto) {
        Application application = getById(applicationDto.getId());
        application.setStatus(resolveStatus(ApplicationStatusType.REJECTED));

        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> rejectedApplicationNotificationProducer.send(RejectedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateId(savedApplication.getCandidateId())
                .candidateEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);

        return savedApplication;
    }

    @Transactional
    public Application withdrawnApplication(ApplicationRequestDto applicationDto) {
        Application application = getById(applicationDto.getId());
        application.setStatus(resolveStatus(ApplicationStatusType.WITHDRAWN));

        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> withdrawnApplicationProducer.send(WithdrawnApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerId(savedApplication.getEmployerId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);

        return savedApplication;
    }

    @Transactional
    public Application acceptApplication(Long id, Long userId, String userRole) {
        Application application = getById(id);
        if (!"EMPLOYER".equals(userRole) || !application.getEmployerId().equals(userId)) {
            log.warn("Access denied: user {} role {} attempted to accept application {}", userId, userRole, id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the employer who owns this vacancy can accept applications");
        }
        application.setStatus(resolveStatus(ApplicationStatusType.ACCEPTED));
        Application savedApplication = applicationRepository.save(application);

        CompletableFuture.runAsync(() -> acceptedApplicationNotificationProducer.send(AcceptedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateId(savedApplication.getCandidateId())
                .candidateEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);

        return savedApplication;
    }

    @Transactional
    public Application rejectApplicationById(Long id, Long userId, String userRole) {
        Application application = getById(id);
        if (!"EMPLOYER".equals(userRole) || !application.getEmployerId().equals(userId)) {
            log.warn("Access denied: user {} role {} attempted to reject application {}", userId, userRole, id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the employer who owns this vacancy can reject applications");
        }
        application.setStatus(resolveStatus(ApplicationStatusType.REJECTED));
        Application savedApplication = applicationRepository.save(application);
        CompletableFuture.runAsync(() -> rejectedApplicationNotificationProducer.send(RejectedApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .candidateId(savedApplication.getCandidateId())
                .candidateEmail(userRetryClient.getEmailByUserId(savedApplication.getCandidateId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);
        return savedApplication;
    }

    @Transactional
    public Application withdrawApplicationById(Long id, Long userId, String userRole) {
        Application application = getById(id);
        if (!"JOB_SEEKER".equals(userRole) || !application.getCandidateId().equals(userId)) {
            log.warn("Access denied: user {} role {} attempted to withdraw application {}", userId, userRole, id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the candidate who submitted this application can withdraw it");
        }
        application.setStatus(resolveStatus(ApplicationStatusType.WITHDRAWN));
        Application savedApplication = applicationRepository.save(application);
        CompletableFuture.runAsync(() -> withdrawnApplicationProducer.send(WithdrawnApplicationEvent.builder()
                .applicationId(savedApplication.getId())
                .employerId(savedApplication.getEmployerId())
                .employerEmail(userRetryClient.getEmailByUserId(savedApplication.getEmployerId()))
                .vacancyTitle(vacancyRetryClient.getVacancyTitle(savedApplication.getVacancyId()))
                .build()), notificationExecutor);
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
        List<Application> applications = applicationRepository.findByVacancyId(vacancyId);

        List<ApplicationResponseDto> dtos = applications.stream()
                .map(applicationMapper::toDto)
                .toList();

        List<CompletableFuture<ApplicationResponseDto>> futures = dtos.stream()
                .map(dto -> CompletableFuture.supplyAsync(() -> enrichApplicationDto(dto), notificationExecutor))
                .toList();

        return futures.stream()
                .map(f -> {
                    try { return f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception e) {
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                        log.warn("Failed to fetch enriched application DTO: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByCandidate(Long candidateId) {
        List<Application> applications = applicationRepository.findByCandidateId(candidateId);

        List<ApplicationResponseDto> dtos = applications.stream()
                .map(applicationMapper::toDto)
                .toList();

        List<CompletableFuture<ApplicationResponseDto>> futures = dtos.stream()
                .map(dto -> CompletableFuture.supplyAsync(() -> enrichApplicationDto(dto), notificationExecutor))
                .toList();

        return futures.stream()
                .map(f -> {
                    try { return f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception e) {
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                        log.warn("Failed to fetch enriched application DTO: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByStatus(ApplicationStatusType statusType) {
        List<Application> applications = applicationRepository.findByStatus_Code(statusType);

        List<ApplicationResponseDto> dtos = applications.stream()
                .map(applicationMapper::toDto)
                .toList();

        List<CompletableFuture<ApplicationResponseDto>> futures = dtos.stream()
                .map(dto -> CompletableFuture.supplyAsync(() -> enrichApplicationDto(dto), notificationExecutor))
                .toList();

        return futures.stream()
                .map(f -> {
                    try { return f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception e) {
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                        log.warn("Failed to fetch enriched application DTO: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByEmployer(Long employerId) {
        log.info("Fetching applications for employer: {}", employerId);

        List<Application> applications = applicationRepository.findByEmployerId(employerId);

        List<ApplicationResponseDto> dtos = applications.stream()
                .map(applicationMapper::toDto)
                .toList();

        List<CompletableFuture<ApplicationResponseDto>> futures = dtos.stream()
                .map(dto -> CompletableFuture.supplyAsync(() -> enrichApplicationDto(dto), notificationExecutor))
                .toList();

        return futures.stream()
                .map(f -> {
                    try { return f.get(10, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception e) {
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                        log.warn("Failed to fetch enriched application DTO: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
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
                ApplicationStatusType.ACCEPTED
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
                ApplicationStatusType.ACCEPTED
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
            CompletableFuture<String> vacancyTitleFuture = CompletableFuture.supplyAsync(
                () -> vacancyRetryClient.getVacancyTitle(dto.getVacancyId()), notificationExecutor);

            CompletableFuture<String> companyNameFuture = CompletableFuture.supplyAsync(
                () -> companyRetryClient.getCompanyName(dto.getVacancyId()), notificationExecutor);

            CompletableFuture<String> candidateNameFuture = CompletableFuture.supplyAsync(
                () -> userRetryClient.getUsernameByUserId(dto.getCandidateId()), notificationExecutor);

            CompletableFuture<String> candidateEmailFuture = CompletableFuture.supplyAsync(
                () -> userRetryClient.getEmailByUserId(dto.getCandidateId()), notificationExecutor);

            CompletableFuture<String> resumeTitleFuture = CompletableFuture.supplyAsync(
                () -> resumeRetryClient.getResumeTitle(dto.getResumeId()), notificationExecutor);

            CompletableFuture.allOf(
                vacancyTitleFuture, companyNameFuture,
                candidateNameFuture, candidateEmailFuture, resumeTitleFuture
            ).get(5, java.util.concurrent.TimeUnit.SECONDS);

            dto.setVacancyTitle(vacancyTitleFuture.getNow("Unknown Vacancy"));
            dto.setCompanyName(companyNameFuture.getNow("Unknown Company"));
            dto.setCandidateName(candidateNameFuture.getNow("Unknown User"));
            dto.setCandidateEmail(candidateEmailFuture.getNow(null));
            dto.setResumeTitle(resumeTitleFuture.getNow("Resume"));

        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Failed to enrich application DTO {}: {}", dto.getId(), e.getMessage());
        }

        return dto;
    }
}