package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptedApplicationEvent {
    private Long applicationId;
    private Long candidateId;
    private String candidateEmail;
    private String vacancyTitle;
}
