package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewApplicationEvent {
    private Long applicationId;
    private String employerEmail;
    private String vacancyTitle;
}
