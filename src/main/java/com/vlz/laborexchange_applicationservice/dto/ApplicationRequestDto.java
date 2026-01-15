package com.vlz.laborexchange_applicationservice.dto;

import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private String StatusCode;
}