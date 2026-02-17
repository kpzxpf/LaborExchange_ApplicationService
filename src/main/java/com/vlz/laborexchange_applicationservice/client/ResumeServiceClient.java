package com.vlz.laborexchange_applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${spring.clients.resume-service.name}", url = "${spring.clients.resume-service.url}")
public interface ResumeServiceClient {

    @GetMapping("/api/resumes/{id}/title")
    String getResumeTitle(@PathVariable Long id);
}