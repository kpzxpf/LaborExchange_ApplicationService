package com.vlz.laborexchange_applicationservice.controller;

import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationStatisticsDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
import com.vlz.laborexchange_applicationservice.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationMapper applicationMapper;

    @PostMapping
    public ApplicationResponseDto create(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.createApplication(requestDto);
        return applicationMapper.toDto(saved);
    }

    @GetMapping("/{id}")
    public ApplicationResponseDto getById(@PathVariable Long id) {
        Application entity = applicationService.getById(id);
        return applicationMapper.toDto(entity);
    }

    @GetMapping("/vacancy/{vacancyId}")
    public List<ApplicationResponseDto> getByVacancy(@PathVariable Long vacancyId) {
        return applicationService.getApplicationsByVacancy(vacancyId);
    }

    @GetMapping("/candidate/{candidateId}")
    public List<ApplicationResponseDto> getByCandidate(@PathVariable Long candidateId) {
        return applicationService.getApplicationsByCandidate(candidateId);
    }

    @PostMapping("/reject")
    public ApplicationResponseDto rejectApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.rejectApplication(requestDto);

        return applicationMapper.toDto(saved);
    }

    @PostMapping("/withdrawn")
    public ApplicationResponseDto withdrawnApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.withdrawnApplication(requestDto);

        return applicationMapper.toDto(saved);
    }

    @GetMapping("/employer/{employerId}")
    public List<ApplicationResponseDto> getByEmployer(@PathVariable Long employerId) {
        return applicationService.getApplicationsByEmployer(employerId);
    }

    @GetMapping("/status/{status}")
    public List<ApplicationResponseDto> getByStatus(@PathVariable String status) {
        ApplicationStatusType statusType = ApplicationStatusType.valueOf(status.toUpperCase());

        return applicationService.getApplicationsByStatus(statusType);
    }

    @GetMapping("/statistics")
    public ApplicationStatisticsDto getStatistics() {
        return applicationService.getStatistics();
    }

    @GetMapping("/employer/{employerId}/statistics")
    public ApplicationStatisticsDto getEmployerStatistics(@PathVariable Long employerId) {
        return applicationService.getEmployerStatistics(employerId);
    }
}