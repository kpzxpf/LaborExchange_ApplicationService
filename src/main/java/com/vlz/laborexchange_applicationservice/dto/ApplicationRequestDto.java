package com.vlz.laborexchange_applicationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequestDto {
    @NotNull
    private Long vacancyId;

    @NotNull
    private Long employerId;

    @NotNull
    private Long candidateId;

    @NotNull
    private Long resumeId;

    @Size(max = 1000)
    private String coverLetter;
}