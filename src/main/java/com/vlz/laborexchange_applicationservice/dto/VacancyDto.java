package com.vlz.laborexchange_applicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyDto {
    private long id;

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Description required")
    @Size(max = 5000, message = "The description should not exceed 5000 characters.")
    private String description;

    @NotBlank(message = "Company name is required")
    private String companyName;
}
