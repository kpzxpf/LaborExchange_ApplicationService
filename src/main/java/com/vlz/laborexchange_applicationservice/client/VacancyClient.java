package com.vlz.laborexchange_applicationservice.client;

import com.vlz.laborexchange_applicationservice.dto.VacancyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${spring.clients.vacancy-service.name}",
        url = "${spring.clients.vacancy-service.url}"
)
public interface VacancyClient {
    @GetMapping("/api/vacancies/{id}")
    VacancyDto getById(@PathVariable Long id);
}