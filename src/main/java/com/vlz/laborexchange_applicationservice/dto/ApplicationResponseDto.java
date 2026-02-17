package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDto {
    private Long id;
    private Long vacancyId;
    private Long candidateId;
    private Long resumeId;
    private Long employerId;
    private String statusName;
    private String vacancyTitle;
    private String companyName;
    private String candidateName;
    private String candidateEmail;
    private String resumeTitle;
    private LocalDateTime createdAt;
}
