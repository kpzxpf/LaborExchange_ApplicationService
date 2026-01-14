package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent {
    private Long applicationId;
    private Long employerId;
    private Long candidateId;
    private String vacancyTitle;
    private String statusCode;
}
