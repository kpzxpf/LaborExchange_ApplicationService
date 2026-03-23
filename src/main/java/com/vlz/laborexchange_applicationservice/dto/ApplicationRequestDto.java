package com.vlz.laborexchange_applicationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for creating or modifying a job application")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequestDto {

    @Schema(description = "Application ID (used only for legacy update endpoints)", example = "42")
    private Long id;

    @Schema(description = "Vacancy the candidate is applying to", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long vacancyId;

    @Schema(description = "Employer who posted the vacancy", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long employerId;

    @Schema(description = "Candidate submitting the application (overridden by X-User-Id on POST /api/applications)", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long candidateId;

    @Schema(description = "Resume the candidate is attaching to this application", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long resumeId;

    @Schema(description = "Optional cover letter from the candidate", example = "I am very interested in this position...")
    private String coverLetter;
}
