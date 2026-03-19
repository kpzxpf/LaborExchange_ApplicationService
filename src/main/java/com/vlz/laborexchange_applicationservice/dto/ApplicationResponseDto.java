package com.vlz.laborexchange_applicationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Job application enriched with data from Vacancy, Company, User, and Resume services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDto {

    @Schema(description = "Application ID", example = "42")
    private Long id;

    @Schema(description = "Vacancy ID", example = "7")
    private Long vacancyId;

    @Schema(description = "Candidate (job seeker) user ID", example = "15")
    private Long candidateId;

    @Schema(description = "Resume ID attached to this application", example = "9")
    private Long resumeId;

    @Schema(description = "Employer user ID", example = "3")
    private Long employerId;

    @Schema(description = "Human-readable status name", example = "Новая")
    private String statusName;

    @Schema(description = "Machine-readable status code", example = "NEW", allowableValues = {"NEW", "ACCEPTED", "REJECTED", "WITHDRAWN"})
    private String statusCode;

    @Schema(description = "Vacancy title fetched from VacancyService (null if service unavailable)", example = "Backend Developer")
    private String vacancyTitle;

    @Schema(description = "Company name fetched from VacancyService (null if service unavailable)", example = "Acme Corp")
    private String companyName;

    @Schema(description = "Candidate full name fetched from UserService (null if service unavailable)", example = "Ivan Petrov")
    private String candidateName;

    @Schema(description = "Candidate email fetched from UserService (null if service unavailable)", example = "ivan@example.com")
    private String candidateEmail;

    @Schema(description = "Resume title fetched from ResumeService (null if service unavailable)", example = "Java Developer CV")
    private String resumeTitle;

    @Schema(description = "Timestamp when the application was created", example = "2026-03-20T12:00:00")
    private LocalDateTime createdAt;
}
