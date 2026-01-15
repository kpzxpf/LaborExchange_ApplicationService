package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedApplicationEvent {
    private Long applicationId;
    private String candidateEmail;
    private String vacancyTitle;
}
