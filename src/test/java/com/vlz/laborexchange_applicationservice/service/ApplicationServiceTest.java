package com.vlz.laborexchange_applicationservice.service;

import com.vlz.laborexchange_applicationservice.dto.NewApplicationEvent;
import com.vlz.laborexchange_applicationservice.dto.WithdrawnApplicationEvent;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.exception.DuplicateApplicationException;
import com.vlz.laborexchange_applicationservice.producer.NewApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.RejectedApplicationNotificationProducer;
import com.vlz.laborexchange_applicationservice.producer.WithdrawnApplicationProducer;
import com.vlz.laborexchange_applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private NewApplicationNotificationProducer newApplicationNotificationProducer;
    @Mock private RejectedApplicationNotificationProducer rejectedApplicationNotificationProducer;
    @Mock private WithdrawnApplicationProducer withdrawnApplicationProducer;
    @Mock private VacancyRetryClient vacancyRetryClient;
    @Mock private UserRetryClient userRetryClient;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    @DisplayName("createApplication: успех — сохранение и отправка уведомления")
    void createApplication_Success() {
        // Arrange
        Application app = Application.builder()
                .id(1L)
                .vacancyId(10L)
                .candidateId(20L)
                .employerId(30L)
                .resumeId(40L)
                .build();

        when(applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(10L, 20L, 40L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(app);
        when(userRetryClient.getEmailByUserId(30L)).thenReturn("employer@test.com");
        when(vacancyRetryClient.getVacancyTitle(10L)).thenReturn("Java Developer");

        // Act
        Application result = applicationService.createApplication(app);

        // Assert
        assertNotNull(result);
        assertEquals(ApplicationStatusType.NEW, result.getStatusFromType());
        verify(newApplicationNotificationProducer).send(any(NewApplicationEvent.class));
        verify(applicationRepository).save(app);
    }

    @Test
    @DisplayName("createApplication: ошибка — дубликат заявки")
    void createApplication_Duplicate_ThrowsException() {
        // Arrange
        Application app = Application.builder().vacancyId(1L).candidateId(1L).resumeId(1L).build();
        when(applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(1L, 1L, 1L)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateApplicationException.class, () -> applicationService.createApplication(app));
        verify(applicationRepository, never()).save(any());
        verifyNoInteractions(newApplicationNotificationProducer);
    }

    @Test
    @DisplayName("rejectApplication: успех — смена статуса и уведомление кандидата")
    void rejectApplication_Success() {
        // Arrange
        Application app = Application.builder()
                .id(1L)
                .candidateId(20L)
                .vacancyId(10L)
                .build();

        when(applicationRepository.save(any(Application.class))).thenReturn(app);
        when(userRetryClient.getEmailByUserId(20L)).thenReturn("candidate@test.com");
        when(vacancyRetryClient.getVacancyTitle(10L)).thenReturn("Java Developer");

        // Act
        Application result = applicationService.rejectApplication(app);

        // Assert
        assertEquals(ApplicationStatusType.REJECTED, result.getStatusFromType());
        verify(rejectedApplicationNotificationProducer).send(argThat(event ->
                event.getCandidateEmail().equals("candidate@test.com")
        ));
    }

    @Test
    @DisplayName("withdrawnApplication: успех — отзыв заявки кандидатом")
    void withdrawnApplication_Success() {
        // Arrange
        Application app = Application.builder()
                .id(1L)
                .candidateId(20L)
                .vacancyId(10L)
                .build();

        when(applicationRepository.save(any(Application.class))).thenReturn(app);
        when(userRetryClient.getEmailByUserId(20L)).thenReturn("candidate@test.com");

        // Act
        Application result = applicationService.withdrawnApplication(app);

        // Assert
        assertEquals(ApplicationStatusType.WITHDRAWN, result.getStatusFromType());
        verify(withdrawnApplicationProducer).send(any(WithdrawnApplicationEvent.class));
    }
}