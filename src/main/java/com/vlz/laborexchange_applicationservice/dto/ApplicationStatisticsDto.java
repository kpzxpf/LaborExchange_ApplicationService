package com.vlz.laborexchange_applicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatisticsDto {
    private Long totalApplications;
    private Map<String, Long> applicationsByStatus;
    private Long activeApplications;
    private Double withdrawalRate;
}