package com.vlz.laborexchange_applicationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Application Service API")
                        .version("1.0.0")
                        .description("""
                                Manages the full lifecycle of job applications.

                                **Application statuses:** `NEW` → `ACCEPTED` / `REJECTED` / `WITHDRAWN`

                                **Headers injected by API Gateway (required for protected endpoints):**
                                - `X-User-Id` — authenticated user ID
                                - `X-User-Role` — `EMPLOYER` or `JOB_SEEKER`

                                **Notifications:** status transitions publish events to Kafka topic \
`notification-events`, consumed by NotificationService for email delivery.

                                **Database:** PostgreSQL (`applicationdb`, port 5437).
                                """)
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained via POST /api/auth/login")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Direct"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")))
                .tags(List.of(
                        new Tag().name("Applications").description("CRUD and status transitions for job applications"),
                        new Tag().name("Statistics").description("Aggregated application statistics")));
    }
}
