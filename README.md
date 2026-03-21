# Application Service

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.6-brightgreen?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Port](https://img.shields.io/badge/port-8085-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-applicationdb-336791?logo=postgresql)
![Kafka](https://img.shields.io/badge/Kafka-notification--events-231F20?logo=apachekafka)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

Microservice managing the full lifecycle of job applications, from submission to resolution.

## Table of Contents

- [Overview](#overview)
- [Application Lifecycle](#application-lifecycle)
- [API Endpoints](#api-endpoints)
- [Data Models](#data-models)
- [Kafka Events](#kafka-events)
- [Configuration](#configuration)
- [Running Locally](#running-locally)

## Overview

| Property | Value |
|---|---|
| Port | **8085** |
| Base path | `/api/applications` |
| Database | PostgreSQL — `applicationdb` (port 5437) |
| Migrations | Flyway |
| Swagger UI | `http://localhost:8085/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8085/v3/api-docs` |
| Prometheus | `http://localhost:8085/actuator/prometheus` |

## Application Lifecycle

```
        submit
JOB_SEEKER ──────────► NEW
                         │
              ┌──────────┼──────────┐
         accept │    reject │   withdraw │
              ▼          ▼            ▼
         ACCEPTED     REJECTED    WITHDRAWN
```

## API Endpoints

### Applications — `/api/applications`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/` | JWT | Submit a job application |
| `GET` | `/{id}` | No | Get application by ID (enriched) |
| `GET` | `/my` | JWT | Get my applications |
| `GET` | `/vacancy/{vacancyId}` | No | Get applications for vacancy |
| `GET` | `/candidate/{candidateId}` | No | Get applications by candidate |
| `GET` | `/employer/{employerId}` | No | Get all employer applications |
| `GET` | `/status/{status}` | No | Get applications by status |
| `PATCH` | `/{id}/accept` | `EMPLOYER` | Accept application |
| `PATCH` | `/{id}/reject` | `EMPLOYER` | Reject application |
| `PATCH` | `/{id}/withdraw` | `JOB_SEEKER` | Withdraw application |
| `GET` | `/statistics` | No | Global statistics |
| `GET` | `/employer/{employerId}/statistics` | No | Employer statistics |

### Status values

| Status | Description |
|---|---|
| `NEW` | Application submitted, awaiting review |
| `ACCEPTED` | Employer accepted the application |
| `REJECTED` | Employer rejected the application |
| `WITHDRAWN` | Candidate withdrew the application |

## Data Models

### ApplicationRequestDto

| Field | Type | Description |
|---|---|---|
| `vacancyId` | Long | Target vacancy ID |
| `employerId` | Long | Vacancy employer ID |
| `resumeId` | Long | Candidate's resume ID |

`candidateId` is taken from the `X-User-Id` header, not the request body.

### ApplicationResponseDto

| Field | Type | Description |
|---|---|---|
| `id` | Long | Application ID |
| `vacancyId` | Long | |
| `candidateId` | Long | |
| `resumeId` | Long | |
| `employerId` | Long | |
| `statusCode` | String | `NEW`, `ACCEPTED`, `REJECTED`, `WITHDRAWN` |
| `statusName` | String | Localised status name |
| `vacancyTitle` | String | Fetched from VacancyService |
| `companyName` | String | Fetched from VacancyService |
| `candidateName` | String | Fetched from UserService |
| `candidateEmail` | String | Fetched from UserService |
| `resumeTitle` | String | Fetched from ResumeService |
| `createdAt` | LocalDateTime | |

### ApplicationStatisticsDto

| Field | Type | Description |
|---|---|---|
| `totalApplications` | Long | Total count |
| `applicationsByStatus` | Map\<String,Long\> | Count per status |
| `activeApplications` | Long | NEW + ACCEPTED |
| `withdrawalRate` | Double | 0.0–1.0 |

## Kafka Events

| Topic | Trigger | Consumed by |
|---|---|---|
| `notification-events` | Application created, rejected, withdrawn | NotificationService |

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8085` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5437/applicationdb` | Database URL |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka brokers |

## Running Locally

```bash
./gradlew bootRun
```

Requires PostgreSQL on port 5437 (`applicationdb`) and Kafka. VacancyService, UserService, and ResumeService must be running for enriched responses.
