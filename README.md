# 📋 LaborExchange Application Service

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=for-the-badge&logo=spring)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Producer-black?style=for-the-badge&logo=apache-kafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)

**Job Application Tracking Service**

</div>

---

## 📋 Overview

Application Service manages job applications, tracking their status through the hiring process. It publishes events to Kafka for notifications and provides application statistics.

### Key Features

✅ **Application Management** - Create, update, withdraw applications  
✅ **Status Tracking** - NEW → REJECTED/WITHDRAWN workflow  
✅ **Event Publishing** - Kafka events for notifications  
✅ **Statistics** - Application counts by status  
✅ **Duplicate Prevention** - Unique constraint on (vacancy, candidate, resume)  

## 🏗️ Architecture

**Service:** Port 8085  
**Database:** PostgreSQL on port 5437  

### System Flow

```
Client → Application Service → Database
                ↓
         Kafka Events → Notification Service
```

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| HTTP Client | OpenFeign |

## 📡 API Endpoints

### Application Endpoints

#### Create Application

```http
POST /api/applications

{
  "vacancyId": 1,
  "candidateId": 123,
  "resumeId": 5,
  "coverLetter": "I am very interested..."
}
```

**Response:**
```json
{
  "id": 1,
  "vacancyId": 1,
  "candidateId": 123,
  "resumeId": 5,
  "status": "NEW",
  "coverLetter": "I am very interested...",
  "createdAt": "2024-02-05T10:30:00Z"
}
```

**Events Published:**
- Topic: `new-application-notification`
- Payload: ApplicationEvent with all details

#### Get Application by ID

```http
GET /api/applications/{id}
```

#### Get All Applications

```http
GET /api/applications?page=0&size=20&status=NEW
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `status`: Filter by status (NEW, REJECTED, WITHDRAWN)

#### Get Applications by Vacancy

```http
GET /api/applications/vacancy/{vacancyId}
```

#### Get Applications by Candidate

```http
GET /api/applications/candidate/{candidateId}
```

#### Reject Application

```http
POST /api/applications/{id}/reject

{
  "reason": "Position filled"
}
```

**Events Published:**
- Topic: `rejected-application-notification`

#### Withdraw Application

```http
POST /api/applications/{id}/withdraw
```

**Events Published:**
- Topic: `withdrawn-application-notification`

#### Get Statistics

```http
GET /api/applications/statistics
```

**Response:**
```json
{
  "total": 150,
  "new": 45,
  "rejected": 80,
  "withdrawn": 25,
  "byStatus": {
    "NEW": 45,
    "REJECTED": 80,
    "WITHDRAWN": 25
  }
}
```

## 🗄️ Database Schema

```sql
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    cover_letter TEXT,
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_application 
        UNIQUE (vacancy_id, candidate_id, resume_id),
    CONSTRAINT check_status 
        CHECK (status IN ('NEW', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_applications_vacancy ON applications(vacancy_id);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_status ON applications(status);
```

### Entity

```java
@Entity
@Table(name = "applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vacancy_id", nullable = false)
    private Long vacancyId;
    
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;
    
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.NEW;
    
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Status Enum

```java
public enum ApplicationStatus {
    NEW,        // Application submitted
    REJECTED,   // Rejected by employer
    WITHDRAWN   // Withdrawn by candidate
}
```

## 📨 Kafka Integration

### Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Events Published

#### New Application Event

```java
public void createApplication(CreateApplicationRequest request) {
    Application application = applicationRepository.save(
        Application.builder()
            .vacancyId(request.getVacancyId())
            .candidateId(request.getCandidateId())
            .resumeId(request.getResumeId())
            .coverLetter(request.getCoverLetter())
            .build()
    );
    
    // Publish event
    ApplicationEvent event = ApplicationEvent.builder()
        .applicationId(application.getId())
        .vacancyId(application.getVacancyId())
        .candidateId(application.getCandidateId())
        .build();
    
    kafkaTemplate.send("new-application-notification", 
                      objectMapper.writeValueAsString(event));
}
```

#### Rejection Event

```java
public void rejectApplication(Long id, String reason) {
    Application application = findById(id);
    application.setStatus(ApplicationStatus.REJECTED);
    application.setRejectionReason(reason);
    applicationRepository.save(application);
    
    // Publish rejection event
    kafkaTemplate.send("rejected-application-notification",
                      createEvent(application));
}
```

#### Withdrawal Event

```java
public void withdrawApplication(Long id) {
    Application application = findById(id);
    application.setStatus(ApplicationStatus.WITHDRAWN);
    applicationRepository.save(application);
    
    // Publish withdrawal event
    kafkaTemplate.send("withdrawn-application-notification",
                      createEvent(application));
}
```

## 🔐 Validation

### Duplicate Prevention

```java
@Service
public class ApplicationService {
    
    public Application createApplication(CreateApplicationRequest request) {
        // Check for duplicate
        if (applicationRepository.existsByVacancyIdAndCandidateIdAndResumeId(
            request.getVacancyId(),
            request.getCandidateId(),
            request.getResumeId()
        )) {
            throw new DuplicateApplicationException(
                "You have already applied to this vacancy with this resume"
            );
        }
        
        // Create application
        Application application = Application.builder()
            .vacancyId(request.getVacancyId())
            .candidateId(request.getCandidateId())
            .resumeId(request.getResumeId())
            .coverLetter(request.getCoverLetter())
            .status(ApplicationStatus.NEW)
            .build();
        
        return applicationRepository.save(application);
    }
}
```

## ⚙️ Configuration

```yaml
server:
  port: 8085

spring:
  application:
    name: application-service
  
  datasource:
    url: jdbc:postgresql://localhost:5437/applicationservice_db
    username: postgres
    password: postgres
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    topics:
      new-application: new-application-notification
      rejected-application: rejected-application-notification
      withdrawn-application: withdrawn-application-notification

logging:
  level:
    com.vlz.laborexchange_applicationservice: INFO
```

## 🧪 Testing

```java
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    
    @Mock private ApplicationRepository repository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    
    @InjectMocks
    private ApplicationService service;
    
    @Test
    void createApplication_Success() {
        // Arrange
        CreateApplicationRequest request = CreateApplicationRequest.builder()
            .vacancyId(1L)
            .candidateId(123L)
            .resumeId(5L)
            .build();
        
        when(repository.existsByVacancyIdAndCandidateIdAndResumeId(
            anyLong(), anyLong(), anyLong()
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        // Act
        Application result = service.createApplication(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(ApplicationStatus.NEW, result.getStatus());
        verify(kafkaTemplate).send(anyString(), anyString());
    }
    
    @Test
    void createApplication_Duplicate_ThrowsException() {
        // Arrange
        when(repository.existsByVacancyIdAndCandidateIdAndResumeId(
            anyLong(), anyLong(), anyLong()
        )).thenReturn(true);
        
        // Act & Assert
        assertThrows(DuplicateApplicationException.class,
            () -> service.createApplication(new CreateApplicationRequest()));
    }
}
```

## 📊 Monitoring

```bash
# Health
curl http://localhost:8085/actuator/health

# Metrics
curl http://localhost:8085/actuator/metrics/application.created

# Statistics
curl http://localhost:8085/api/applications/statistics
```

## 🚀 Quick Start

```bash
# Start services
docker-compose up -d application-db kafka

# Run service
./gradlew bootRun

# Test
curl -X POST http://localhost:8085/api/applications \
  -H "Content-Type: application/json" \
  -d '{
    "vacancyId": 1,
    "candidateId": 123,
    "resumeId": 5,
    "coverLetter": "I am interested..."
  }'
```

---

<div align="center">


</div>
