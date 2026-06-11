# Spring Boot HW2 — Configuration, Profiles, i18n & Structured Logging

REST API for User and Task management, extending the HW1 (Spring Security) project with
externalized configuration & Spring Profiles, multi-language API responses (i18n), and
structured file logging.

## Tech Stack

- Java 21 / Spring Boot 3.4.4
- Maven
- Spring Data JPA — H2 (dev) / PostgreSQL (prod)
- Spring Security (in-memory users, BCrypt, HTTP Basic + form login)
- Bean Validation (JSR-303)
- SpringDoc OpenAPI (Swagger UI)
- SLF4J via Lombok `@Slf4j` + Logback (rolling file appender)

## Quick Start

```bash
# default (neutral) configuration — in-memory H2, no seed data, INFO logging
./mvnw spring-boot:run
```

The app starts at `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

---

## 1. Profiles — how to run `dev` vs `prod`

Two profiles are defined via `application-dev.properties` and `application-prod.properties`
(the shared base lives in `application.properties`).

| Profile | Database | Seed data | SQL echo | Log level (`com.example.midterm`) |
|---------|----------|-----------|----------|-----------------------------------|
| `dev`   | In-memory **H2** (`jdbc:h2:mem:devdb`) | Yes (`DataInitializer`) | on | `DEBUG` |
| `prod`  | **PostgreSQL** (persistent) | No | off | `WARN` |

### Run with the `dev` profile

```bash
# Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Packaged jar
java -jar target/midterm-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Run with the `prod` profile

`prod` expects a real PostgreSQL database. Provide credentials via environment variables.
`DB_PASSWORD` has **no default** — prod fails fast if it is missing (`DB_URL`/`DB_USERNAME`
have sensible non-secret defaults):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/taskmanager
export DB_USERNAME=taskmanager
export DB_PASSWORD=yourpassword

./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
# or
java -jar target/midterm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### From an IDE (IntelliJ / Eclipse / VS Code)

Set the active profile on the run configuration, e.g. the VM option
`-Dspring.profiles.active=dev`, or the environment variable `SPRING_PROFILES_ACTIVE=dev`.

---

## 2. Custom Configuration Properties

Defined in `config/AppSettings.java` — a `@ConfigurationProperties(prefix = "app.settings")`
POJO, validated at startup with JSR-303 constraints + `@Validated`. Values are supplied
per-profile and injected into `InfoController`.

| Property | Type | Validation | Role |
|----------|------|------------|------|
| `app.settings.title` | String | `@NotBlank` | Application title shown on metadata endpoint |
| `app.settings.pagination-limit` | int | `@Min(1)` | Caps the number of items returned by `GET /api/users` and `GET /api/tasks` |
| `app.settings.contact-email` | String | `@NotBlank`, `@Email` | Support contact address |
| `app.settings.registration-enabled` | boolean | — | Feature flag advertising self-service registration |

Injection is demonstrated in two places:

- **`GET /api/info`** (`InfoController`) — the response reflects the active profile's values;
- **`UserService` / `TaskService`** — `pagination-limit` caps the size of the lists returned
  by `GET /api/users` and `GET /api/tasks` (10 items under `dev`, 50 under `prod`).

```bash
curl http://localhost:8080/api/info
# {"message":"Welcome to Task Manager API (DEV)","title":"Task Manager API (DEV)",
#  "paginationLimit":10,"contactEmail":"dev-team@example.com",
#  "registrationEnabled":true,"activeProfiles":["dev"]}
```

If any constraint is violated (e.g. a blank title or non-email contact), the application
**fails to start** with a clear binding/validation error.

---

## 3. Internationalization (i18n)

Message bundles under `src/main/resources` (UTF-8):

- `messages.properties` — default (English fallback)
- `messages_en.properties` — English
- `messages_ka.properties` — Georgian (ქართული)

Locale is resolved per request from the standard **`Accept-Language`** HTTP header via an
`AcceptHeaderLocaleResolver` bean (`config/I18nConfig.java`); English is the default.

### What is internationalized

| Where | Keys | Notes |
|-------|------|-------|
| `GET /api/info` welcome message | `app.welcome` | Injected via `MessageSource` |
| `404` not-found payload | `error.notfound`, `error.notfound.title` | In `@RestControllerAdvice` |
| `400` validation payload titles | `error.validation.title`, `error.badrequest.title` | In `@RestControllerAdvice` |
| Field validation messages | `user.name.*`, `user.email.*`, `task.title.*` | Referenced from DTO constraints with the `{key}` syntax |

The `MessageSource` is wired into the Bean Validation interpolator
(`LocalValidatorFactoryBean` in `I18nConfig`), so DTO constraint messages like
`@NotBlank(message = "{user.name.required}")` resolve from the same bundles.

### How to test

```bash
# English (default)
curl -H "Accept-Language: en" http://localhost:8080/api/info

# Georgian
curl -H "Accept-Language: ka" http://localhost:8080/api/info
# {"message":"კეთილი იყოს თქვენი მობრძანება Task Manager API (DEV)-ში", ...}

# Localized validation errors (Georgian)
curl -u admin:admin123 -H "Content-Type: application/json" -H "Accept-Language: ka" \
     -d '{"name":"","email":"bad"}' http://localhost:8080/api/users
# {"fieldErrors":{"name":"სახელის მითითება სავალდებულოა","email":"ელფოსტა უნდა იყოს ვალიდური"}, ...}

# Localized 404 (Georgian)
curl -u admin:admin123 -H "Accept-Language: ka" http://localhost:8080/api/users/99999
# {"error":"ვერ მოიძებნა","message":"User ვერ მოიძებნა id-ით: 99999", ...}
```

---

## 4. Structured Logging

- **SLF4J via Lombok's `@Slf4j`** in 5 components: `UserService`, `TaskService`,
  `InfoController`, `DataInitializer`, and `GlobalExceptionHandler`.
- Proper levels: `DEBUG` (troubleshooting), `INFO` (business events — user/task creation),
  `WARN` (not-found / validation failures).
- **Parameterized** logging only — e.g. `log.info("Created user id={}", user.getId());`
  (no string concatenation).
- **Profile-driven levels** via `logback-spring.xml` `<springProfile>` blocks:
  `dev` → `DEBUG`, `prod` → `WARN`, otherwise `INFO`.

### Log file location

```
logs/app.log
```

Configured in `src/main/resources/logback-spring.xml` with a `RollingFileAppender` using a
`SizeAndTimeBasedRollingPolicy` (daily rotation, 10 MB per file, 7 days history, 100 MB cap).
Rolled files are named `logs/app.YYYY-MM-DD.N.log`. Logs are written to the file **and** the
console simultaneously.

---

## Security (carried over from HW1)

In-memory users with BCrypt-hashed passwords:

| Username | Password   | Roles            |
|----------|------------|------------------|
| `user`   | `user123`  | `USER`           |
| `admin`  | `admin123` | `USER`, `ADMIN`  |

Public endpoints: `/`, `/api/info`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`.
All other `/api/**` endpoints require authentication. `DELETE /api/users/**`,
`DELETE /api/tasks/**` and `/h2-console/**` require the `ADMIN` role (also enforced at the
service layer with `@PreAuthorize`).

## API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET    | `/api/info` | App metadata & config (i18n) | public |
| GET    | `/api/me` | Authenticated user's profile | authenticated |
| POST/GET/PUT/DELETE | `/api/users[/{id}]` | User CRUD | authenticated (DELETE = ADMIN) |
| POST/GET/PUT/DELETE | `/api/tasks[/{id}]` | Task CRUD | authenticated (DELETE = ADMIN) |

## Tests

```bash
./mvnw test
```

37 integration tests covering CRUD, validation (400), missing resources (404),
authentication, role authorization, BCrypt storage, method security, i18n
(`Accept-Language`-driven responses and localized validation/404 payloads —
`I18nAndConfigTests`), the `pagination-limit` cap, and dev-profile behavior
(seed data, dev configuration values — `DevProfileTests`).
