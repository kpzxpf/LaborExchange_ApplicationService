package com.vlz.laborexchange_applicationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema(description = "Aggregated statistics for job applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatisticsDto {

    @Schema(description = "Total number of applications", example = "150")
    private Long totalApplications;

    @Schema(description = "Number of applications grouped by status code", example = "{\"NEW\": 80, \"ACCEPTED\": 30, \"REJECTED\": 25, \"WITHDRAWN\": 15}")
    private Map<String, Long> applicationsByStatus;

    @Schema(description = "Number of applications in NEW or ACCEPTED status (not terminal)", example = "110")
    private Long activeApplications;

    @Schema(description = "Ratio of WITHDRAWN applications to total (0.0 – 1.0)", example = "0.10")
    private Double withdrawalRate;
}
