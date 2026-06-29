# Final Project Requirements

Finalize and improve existing Spring Boot REST API project developed throughout the semester.
NOT a new project — must be based on previously submitted application (Midterm + assignments).
All prior functionality must remain fully functional. Goal: production-ready Spring Boot app.

## Existing Functionality (must keep working)

Layered Architecture (Controller, Service, Repository), REST CRUD, DB Integration (JPA/JDBC),
DTOs, Validation, Global Exception Handling, Swagger/OpenAPI, Spring Security, Auth & Authz,
Spring Profiles, Externalized Configuration, i18n, Structured Logging.
Breaking/removing prior features loses points.

## New Requirements

### 1. Application Testing
- Unit Tests (JUnit + Mockito)
- Integration Tests
- Controller Layer Tests (@WebMvcTest) or Repository Layer Tests (@DataJpaTest)
- Positive and negative scenarios
- Encouraged: Parameterized Tests, Test Lifecycle Methods, MockMvc, TestRestTemplate/REST-assured, JaCoCo

### 2. Application Monitoring
Integrate Spring Boot Actuator. Minimum endpoints:
- /actuator/health
- /actuator/info
- /actuator/metrics
Include: proper endpoint config, appropriate security for actuator endpoints, functional health & info.
Encouraged: Custom Health Indicators, Custom Metrics (Micrometer), Prometheus.

### 3. Logging Improvements
SLF4J/Logback, proper levels, parameterized logging, console + file logging,
Rolling Policy, profile-specific logging config.

### 4. Project Quality
Code organization, duplication, exception handling, validation, API response consistency,
package structure, Spring Boot best practices.

### 5. Documentation (README.md)
Project description, technologies, run instructions, user credentials & roles,
testing instructions, monitoring endpoints, logging config, profile config.

## Submission
- Complete runnable project as ZIP (required)
- GitHub/GitLab link (optional)
- Must compile and run without errors.

## Dates
Start: 2026-06-25 13:45:00
End:   2026-06-29 20:30:00
