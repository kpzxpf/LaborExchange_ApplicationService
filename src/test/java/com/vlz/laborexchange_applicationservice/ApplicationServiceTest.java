package com.vlz.laborexchange_applicationservice;

import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationStatisticsDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatus;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.exception.DuplicateApplicationException;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
import com.vlz.laborexchange_applicationservice.producer.NewApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.RejectedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.WithdrawnApplicationProducer;
import com.vlz.laborexchange_applicationservice.repository.ApplicationRepository;
import com.vlz.laborexchange_applicationservice.service.ApplicationService;
import com.vlz.laborexchange_applicationservice.service.CompanyRetryClient;
import com.vlz.laborexchange_applicationservice.service.ResumeRetryClient;
import com.vlz.laborexchange_applicationservice.service.UserRetryClient;
import com.vlz.laborexchange_applicationservice.service.VacancyRetryClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private NewApplicationNotificationProducer newApplicationNotificationProducer;
    @Mock private RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer;
    @Mock private WithdrawnApplicationProducer withdrawnApplicationProducer;
    @Mock private VacancyRetryClient vacancyRetryClient;
    @Mock private UserRetryClient userRetryClient;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private ResumeRetryClient resumeRetryClient;
    @Mock private CompanyRetryClient companyRetryClient;

    private ApplicationService applicationService;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        applicationService = new ApplicationService(
                applicationRepository,
                newApplicationNotificationProducer,
                rejectedApplicationNotificationProducer,
                withdrawnApplicationProducer,
                vacancyRetryClient,
                userRetryClient,
                applicationMapper,
                resumeRetryClient,
                companyRetryClient,
                executor
        );
    }

    private Application buildApplication(Long id, ApplicationStatusType statusType) {
        ApplicationStatus status = new ApplicationStatus();
        status.setId(statusType.getId());
        status.setCode(statusType);
        status.setName(statusType.name());

        return Application.builder()
                .id(id)
                .vacancyId(10L)
                .candidateId(20L)
                .employerId(30L)
                .resumeId(40L)
                .status(status)
                .build();
    }

    @Test
    void createApplication_success() {
        ApplicationRequestDto dto = ApplicationRequestDto.builder()
                .vacancyId(10L).candidateId(20L).employerId(30L).resumeId(40L).build();

        Application saved = buildApplication(1L, ApplicationStatusType.NEW);
        when(applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(anyLong(), anyLong(), anyLong()))
                .thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);
        when(userRetryClient.getEmailByUserId(anyLong())).thenReturn("test@test.com");
        when(vacancyRetryClient.getVacancyTitle(anyLong())).thenReturn("Java Developer");

        Application result = applicationService.createApplication(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatusFromType()).isEqualTo(ApplicationStatusType.NEW);
    }

    @Test
    void createApplication_throwsDuplicateException_whenAlreadyApplied() {
        ApplicationRequestDto dto = ApplicationRequestDto.builder()
                .vacancyId(10L).candidateId(20L).employerId(30L).resumeId(40L).build();

        when(applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(10L, 20L, 40L))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void acceptApplication_changesStatusToAccepted() {
        Application application = buildApplication(1L, ApplicationStatusType.NEW);
        Application accepted = buildApplication(1L, ApplicationStatusType.ACCEPTED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(accepted);

        Application result = applicationService.acceptApplication(1L);

        assertThat(result.getStatusFromType()).isEqualTo(ApplicationStatusType.ACCEPTED);
    }

    @Test
    void rejectApplicationById_changesStatusToRejected() {
        Application application = buildApplication(1L, ApplicationStatusType.NEW);
        Application rejected = buildApplication(1L, ApplicationStatusType.REJECTED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(rejected);
        when(userRetryClient.getEmailByUserId(anyLong())).thenReturn("cand@test.com");
        when(vacancyRetryClient.getVacancyTitle(anyLong())).thenReturn("Title");

        Application result = applicationService.rejectApplicationById(1L);

        assertThat(result.getStatusFromType()).isEqualTo(ApplicationStatusType.REJECTED);
    }

    @Test
    void withdrawApplicationById_changesStatusToWithdrawn() {
        Application application = buildApplication(1L, ApplicationStatusType.NEW);
        Application withdrawn = buildApplication(1L, ApplicationStatusType.WITHDRAWN);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenReturn(withdrawn);
        when(userRetryClient.getEmailByUserId(anyLong())).thenReturn("emp@test.com");
        when(vacancyRetryClient.getVacancyTitle(anyLong())).thenReturn("Title");

        Application result = applicationService.withdrawApplicationById(1L);

        assertThat(result.getStatusFromType()).isEqualTo(ApplicationStatusType.WITHDRAWN);
    }

    @Test
    void getById_throwsEntityNotFound_whenMissing() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getStatistics_returnsCorrectData() {
        when(applicationRepository.count()).thenReturn(10L);
        when(applicationRepository.countByStatusGrouped()).thenReturn(List.of(
                new Object[]{ApplicationStatusType.NEW, 5L},
                new Object[]{ApplicationStatusType.REJECTED, 3L},
                new Object[]{ApplicationStatusType.WITHDRAWN, 2L}
        ));
        when(applicationRepository.countByStatusIn(any())).thenReturn(10L);

        ApplicationStatisticsDto stats = applicationService.getStatistics();

        assertThat(stats.getTotalApplications()).isEqualTo(10L);
        assertThat(stats.getApplicationsByStatus()).containsKey("NEW");
        assertThat(stats.getApplicationsByStatus().get("NEW")).isEqualTo(5L);
        assertThat(stats.getWithdrawalRate()).isEqualTo(20.0);
    }

    @Test
    void getEmployerStatistics_returnsCorrectData() {
        Long employerId = 30L;
        when(applicationRepository.countByEmployerId(employerId)).thenReturn(4L);
        when(applicationRepository.countByEmployerIdAndStatusGrouped(employerId)).thenReturn(List.of(
                new Object[]{ApplicationStatusType.NEW, 2L},
                new Object[]{ApplicationStatusType.ACCEPTED, 2L}
        ));
        when(applicationRepository.countByEmployerIdAndStatusIn(any(Long.class), any())).thenReturn(2L);

        ApplicationStatisticsDto stats = applicationService.getEmployerStatistics(employerId);

        assertThat(stats.getTotalApplications()).isEqualTo(4L);
        assertThat(stats.getApplicationsByStatus()).containsKey("ACCEPTED");
    }

    @Test
    void getApplicationsByEmployer_returnsList() {
        Application app = buildApplication(1L, ApplicationStatusType.NEW);
        ApplicationResponseDto dto = new ApplicationResponseDto();
        dto.setId(1L);
        dto.setVacancyId(10L);
        dto.setCandidateId(20L);
        dto.setEmployerId(30L);
        dto.setResumeId(40L);

        when(applicationRepository.findByEmployerId(30L)).thenReturn(Optional.of(List.of(app)));
        when(applicationMapper.toDto(app)).thenReturn(dto);
        when(vacancyRetryClient.getVacancyTitle(anyLong())).thenReturn("Title");
        when(companyRetryClient.getCompanyName(anyLong())).thenReturn("Company");
        when(userRetryClient.getUsernameByUserId(anyLong())).thenReturn("User");
        when(userRetryClient.getEmailByUserId(anyLong())).thenReturn("user@test.com");
        when(resumeRetryClient.getResumeTitle(anyLong())).thenReturn("My Resume");

        List<ApplicationResponseDto> result = applicationService.getApplicationsByEmployer(30L);

        assertThat(result).hasSize(1);
    }
}
