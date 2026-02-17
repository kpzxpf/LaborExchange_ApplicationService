package com.vlz.laborexchange_applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${spring.clients.company-service.name}",
        url = "${spring.clients.company-service.url}")
public interface CompanyServiceClient {

    @GetMapping("/api/companies/{id}/company")
    String getCompanyName(@PathVariable Long id);
}
