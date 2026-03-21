package com.vlz.laborexchange_applicationservice.controller;

import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationStatisticsDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
import com.vlz.laborexchange_applicationservice.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Applications", description = "CRUD and status transitions for job applications")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Submit a job application",
            description = "Creates a new application for a vacancy. Candidate ID is taken from the `X-User-Id` header (set by the Gateway). Publishes a NEW notification event to Kafka."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application created",
                    content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error — required fields missing"),
            @ApiResponse(responseCode = "409", description = "Duplicate application — candidate already applied to this vacancy with this resume")
    })
    @PostMapping
    public ApplicationResponseDto create(
            @Valid @RequestBody ApplicationRequestDto requestDto,
            @Parameter(description = "Authenticated user ID (injected by Gateway)", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        requestDto.setCandidateId(userId);
        Application saved = applicationService.createApplication(requestDto);
        return applicationMapper.toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Operation(summary = "Get application by ID", description = "Returns a single application enriched with vacancy title, company name, candidate info, and resume title fetched from other services.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found",
                    content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @GetMapping("/{id}")
    public ApplicationResponseDto getById(
            @Parameter(description = "Application ID", required = true) @PathVariable Long id) {
        Application entity = applicationService.getById(id);
        return applicationMapper.toDto(entity);
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get my applications", description = "Returns all applications submitted by the authenticated candidate (identified via `X-User-Id`).")
    @ApiResponse(responseCode = "200", description = "List of applications",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponseDto.class))))
    @GetMapping("/my")
    public List<ApplicationResponseDto> getMy(
            @Parameter(description = "Authenticated user ID (injected by Gateway)", required = true)
            @RequestHeader("X-User-Id") Long userId) {
        return applicationService.getApplicationsByCandidate(userId);
    }

    @Operation(summary = "Get applications by vacancy", description = "Returns all applications for a specific vacancy. Intended for employer use.")
    @ApiResponse(responseCode = "200", description = "List of applications",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponseDto.class))))
    @GetMapping("/vacancy/{vacancyId}")
    public List<ApplicationResponseDto> getByVacancy(
            @Parameter(description = "Vacancy ID", required = true) @PathVariable Long vacancyId) {
        return applicationService.getApplicationsByVacancy(vacancyId);
    }

    @Operation(summary = "Get applications by candidate")
    @ApiResponse(responseCode = "200", description = "List of applications",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponseDto.class))))
    @GetMapping("/candidate/{candidateId}")
    public List<ApplicationResponseDto> getByCandidate(
            @Parameter(description = "Candidate (user) ID", required = true) @PathVariable Long candidateId) {
        return applicationService.getApplicationsByCandidate(candidateId);
    }

    @Operation(summary = "Get applications by employer", description = "Returns all applications across all vacancies belonging to a given employer.")
    @ApiResponse(responseCode = "200", description = "List of applications",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponseDto.class))))
    @GetMapping("/employer/{employerId}")
    public List<ApplicationResponseDto> getByEmployer(
            @Parameter(description = "Employer (user) ID", required = true) @PathVariable Long employerId) {
        return applicationService.getApplicationsByEmployer(employerId);
    }

    @Operation(summary = "Get applications by status", description = "Filters applications by status. Allowed values: `NEW`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of applications",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApplicationResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @GetMapping("/status/{status}")
    public List<ApplicationResponseDto> getByStatus(
            @Parameter(description = "Application status", schema = @Schema(allowableValues = {"NEW", "ACCEPTED", "REJECTED", "WITHDRAWN"}), required = true)
            @PathVariable String status) {
        ApplicationStatusType statusType = ApplicationStatusType.valueOf(status.toUpperCase());
        return applicationService.getApplicationsByStatus(statusType);
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Accept an application",
            description = "Marks the application as ACCEPTED. Only the employer who owns the vacancy may perform this action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application accepted",
                    content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not the vacancy owner"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/{id}/accept")
    public ApplicationResponseDto accept(
            @Parameter(description = "Application ID", required = true) @PathVariable Long id,
            @Parameter(description = "Authenticated user ID (injected by Gateway)", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Authenticated user role (injected by Gateway)", required = true) @RequestHeader("X-User-Role") String userRole) {
        return applicationMapper.toDto(applicationService.acceptApplication(id, userId, userRole));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Reject an application",
            description = "Marks the application as REJECTED. Only the employer who owns the vacancy may perform this action. Publishes a REJECTED notification event to Kafka."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application rejected",
                    content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not the vacancy owner"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/{id}/reject")
    public ApplicationResponseDto reject(
            @Parameter(description = "Application ID", required = true) @PathVariable Long id,
            @Parameter(description = "Authenticated user ID (injected by Gateway)", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Authenticated user role (injected by Gateway)", required = true) @RequestHeader("X-User-Role") String userRole) {
        return applicationMapper.toDto(applicationService.rejectApplicationById(id, userId, userRole));
    }

    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Withdraw an application",
            description = "Marks the application as WITHDRAWN. Only the candidate who submitted the application may perform this action. Publishes a WITHDRAWN notification event to Kafka."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application withdrawn",
                    content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden — caller is not the application owner"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @PatchMapping("/{id}/withdraw")
    public ApplicationResponseDto withdraw(
            @Parameter(description = "Application ID", required = true) @PathVariable Long id,
            @Parameter(description = "Authenticated user ID (injected by Gateway)", required = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Authenticated user role (injected by Gateway)", required = true) @RequestHeader("X-User-Role") String userRole) {
        return applicationMapper.toDto(applicationService.withdrawApplicationById(id, userId, userRole));
    }

    // -------------------------------------------------------------------------
    // Legacy endpoints (kept for backward compatibility)
    // -------------------------------------------------------------------------

    @Operation(summary = "[Legacy] Reject application by body", deprecated = true,
            description = "Use `PATCH /{id}/reject` instead.")
    @ApiResponse(responseCode = "200", description = "Application rejected",
            content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class)))
    @PostMapping("/reject")
    public ApplicationResponseDto rejectApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.rejectApplication(requestDto);
        return applicationMapper.toDto(saved);
    }

    @Operation(summary = "[Legacy] Withdraw application by body", deprecated = true,
            description = "Use `PATCH /{id}/withdraw` instead.")
    @ApiResponse(responseCode = "200", description = "Application withdrawn",
            content = @Content(schema = @Schema(implementation = ApplicationResponseDto.class)))
    @PostMapping("/withdrawn")
    public ApplicationResponseDto withdrawnApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.withdrawnApplication(requestDto);
        return applicationMapper.toDto(saved);
    }

    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------

    @Operation(summary = "Global statistics", description = "Returns aggregated statistics across all applications: total count, count per status, active applications, and withdrawal rate.")
    @ApiResponse(responseCode = "200", description = "Statistics",
            content = @Content(schema = @Schema(implementation = ApplicationStatisticsDto.class)))
    @Tag(name = "Statistics")
    @GetMapping("/statistics")
    public ApplicationStatisticsDto getStatistics() {
        return applicationService.getStatistics();
    }

    @Operation(summary = "Employer statistics", description = "Returns aggregated statistics for a specific employer's vacancies.")
    @ApiResponse(responseCode = "200", description = "Statistics",
            content = @Content(schema = @Schema(implementation = ApplicationStatisticsDto.class)))
    @Tag(name = "Statistics")
    @GetMapping("/employer/{employerId}/statistics")
    public ApplicationStatisticsDto getEmployerStatistics(
            @Parameter(description = "Employer (user) ID", required = true) @PathVariable Long employerId) {
        return applicationService.getEmployerStatistics(employerId);
    }
}
