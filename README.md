📨 Application Service
Overview
Manages job applications and tracks application status.
Port: 8085
Database: PostgreSQL (applicationdb - port 5437)
Key Endpoints
http# Create application
POST /api/applications
{
  "vacancyId": 10,
  "employerId": 5,
  "candidateId": 20,
  "resumeId": 3
}

# Get application by ID
GET /api/applications/1

# Get applications by vacancy
GET /api/applications/vacancy/10

# Get applications by candidate
GET /api/applications/candidate/20

# Get applications by employer
GET /api/applications/employer/5

# Get applications by status
GET /api/applications/status/NEW

# Reject application
POST /api/applications/reject
{
  "vacancyId": 10,
  "employerId": 5,
  "candidateId": 20,
  "resumeId": 3
}

# Withdraw application
POST /api/applications/withdrawn

# Get statistics
GET /api/applications/statistics
Application Statuses

NEW - Newly submitted application
REJECTED - Rejected by employer
WITHDRAWN - Withdrawn by candidate

Kafka Events Published
Topic: new-application-notification
json{
  "applicationId": 1,
  "employerEmail": "hr@company.com",
  "vacancyTitle": "Java Developer"
}
Topic: rejected-application-notification
json{
  "applicationId": 1,
  "candidateEmail": "john@example.com",
  "vacancyTitle": "Java Developer"
}
Topic: withdrawn-application-notification
json{
  "applicationId": 1,
  "employerEmail": "hr@company.com",
  "vacancyTitle": "Java Developer"
}
