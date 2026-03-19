package com.vlz.laborexchange_applicationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vlz.laborexchange_applicationservice.controller.ApplicationController;
import com.vlz.laborexchange_applicationservice.dto.ApplicationRequestDto;
import com.vlz.laborexchange_applicationservice.dto.ApplicationResponseDto;
import com.vlz.laborexchange_applicationservice.entity.Application;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatus;
import com.vlz.laborexchange_applicationservice.entity.ApplicationStatusType;
import com.vlz.laborexchange_applicationservice.mapper.ApplicationMapper;
import com.vlz.laborexchange_applicationservice.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private ApplicationMapper applicationMapper;

    private Application buildApplication(Long id, ApplicationStatusType statusType) {
        ApplicationStatus status = new ApplicationStatus();
        status.setId(statusType.getId());
        status.setCode(statusType);
        status.setName(statusType.name());

        return Application.builder()
                .id(id)
                .vacancyId(10L)
                .candidateId(20L)
                .employerId(30L)
                .resumeId(40L)
                .status(status)
                .build();
    }

    private ApplicationResponseDto buildResponseDto(Long id, ApplicationStatusType statusType) {
        return ApplicationResponseDto.builder()
                .id(id)
                .vacancyId(10L)
                .candidateId(20L)
                .resumeId(40L)
                .employerId(30L)
                .statusName(statusType.name())
                .build();
    }

    @Test
    void createApplication_returns200WithDto() throws Exception {
        ApplicationRequestDto requestDto = ApplicationRequestDto.builder()
                .vacancyId(10L).candidateId(20L).employerId(30L).resumeId(40L).build();

        Application saved = buildApplication(1L, ApplicationStatusType.NEW);
        ApplicationResponseDto responseDto = buildResponseDto(1L, ApplicationStatusType.NEW);

        when(applicationService.createApplication(any(ApplicationRequestDto.class))).thenReturn(saved);
        when(applicationMapper.toDto(saved)).thenReturn(responseDto);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statusName").value("NEW"));
    }

    @Test
    void createApplication_missingRequiredFields_returns400() throws Exception {
        String invalidJson = "{\"vacancyId\": null, \"candidateId\": null, \"employerId\": null, \"resumeId\": null}";

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMy_returns200WithList() throws Exception {
        ApplicationResponseDto dto1 = buildResponseDto(1L, ApplicationStatusType.NEW);
        ApplicationResponseDto dto2 = buildResponseDto(2L, ApplicationStatusType.ACCEPTED);

        when(applicationService.getApplicationsByCandidate(20L)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/applications/my")
                        .header("X-User-Id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].statusName").value("ACCEPTED"));
    }

    @Test
    void acceptApplication_returns200() throws Exception {
        Application accepted = buildApplication(1L, ApplicationStatusType.ACCEPTED);
        ApplicationResponseDto responseDto = buildResponseDto(1L, ApplicationStatusType.ACCEPTED);

        when(applicationService.acceptApplication(1L)).thenReturn(accepted);
        when(applicationMapper.toDto(accepted)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/applications/1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("ACCEPTED"));
    }

    @Test
    void rejectApplication_returns200() throws Exception {
        Application rejected = buildApplication(1L, ApplicationStatusType.REJECTED);
        ApplicationResponseDto responseDto = buildResponseDto(1L, ApplicationStatusType.REJECTED);

        when(applicationService.rejectApplicationById(1L)).thenReturn(rejected);
        when(applicationMapper.toDto(rejected)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/applications/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusName").value("REJECTED"));
    }
}
