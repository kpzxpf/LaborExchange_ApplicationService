package com.vlz.laborexchange_applicationservice.controller;

import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
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
        Application entity = applicationMapper.toEntity(requestDto);

        Application saved = applicationService.createApplication(entity);
        return applicationMapper.toDto(saved);
    }

    @GetMapping("/{id}")
    public ApplicationResponseDto getById(@PathVariable Long id) {
        Application entity = applicationService.getById(id);
        return applicationMapper.toDto(entity);
    }

    @GetMapping("/vacancy/{vacancyId}")
    public List<ApplicationResponseDto> getByVacancy(@PathVariable Long vacancyId) {
        return applicationService.getApplicationsByVacancy(vacancyId).stream()
                .map(applicationMapper::toDto)
                .toList();
    }

    @GetMapping("/candidate/{candidateId}")
    public List<ApplicationResponseDto> getByCandidate(@PathVariable Long candidateId) {
        return applicationService.getApplicationsByCandidate(candidateId).stream()
                .map(applicationMapper::toDto)
                .toList();
    }

    @PostMapping("/reject")
    public ApplicationResponseDto rejectApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.rejectApplication(applicationMapper.toEntity(requestDto));

        return applicationMapper.toDto(saved);
    }

    @PostMapping("/withdrawn")
    public ApplicationResponseDto withdrawnApplication(@Valid @RequestBody ApplicationRequestDto requestDto) {
        Application saved = applicationService.withdrawnApplication(applicationMapper.toEntity(requestDto));

        return applicationMapper.toDto(saved);
    }
}