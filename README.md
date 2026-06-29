# Task Manager REST API — Spring Boot Final Project

Production-leaning REST API for managing **Users** and their **Tasks**, built up across the
semester (Midterm → Security → Configuration/Profiles/i18n/Logging) and finalized here with
**automated testing**, **Actuator monitoring**, and quality/logging improvements.

All previously implemented functionality remains intact:
Layered architecture (Controller → Service → Repository), CRUD REST API, JPA persistence,
DTOs, Bean Validation, global exception handling, Swagger/OpenAPI, Spring Security
(authentication + role authorization), Spring Profiles, externalized configuration, and
internationalization (English / Georgian).

---

## Technologies

- **Java 21**, **Spring Boot 3.4.4**, **Maven**
- Spring Web (REST), Spring Data JPA
- **Thymeleaf** server-rendered UI (forms with data binding, Spring Security dialect)
- **H2** (dev / default, in-memory) · **PostgreSQL** (prod)
- Spring Security (in-memory users, BCrypt, HTTP Basic for the API + form login for the UI, method security)
- Bean Validation (JSR-303 / Hibernate Validator)
- SpringDoc OpenAPI (Swagger UI)
- **SLF4J + Logback** (`@Slf4j`), rolling file appender
- **Spring Boot Actuator** + **Micrometer** (Prometheus registry)
- **Testing:** JUnit 5, Mockito, Spring Boot Test, `@WebMvcTest`, `@DataJpaTest`, MockMvc,
  spring-security-test, **JaCoCo** coverage

---

## Running the application

Default (no profile → in-memory H2, INFO logging):

```bash
./mvnw spring-boot:run
```

App: `http://localhost:8080` · Web UI: `http://localhost:8080/` · Swagger UI: `http://localhost:8080/swagger-ui.html`

### Web UI (Thymeleaf)

Server-rendered pages backed by the same services/DTOs as the REST API:

| Path | Purpose | Auth |
|------|---------|------|
| `/` | Landing page | public |
| `/login` | Custom login form | public |
| `/users`, `/users/new`, `/users/{id}/edit` | User list + create/edit forms | authenticated (delete = ADMIN) |
| `/tasks`, `/tasks/new`, `/tasks/{id}/edit` | Task list + create/edit forms (user dropdown) | authenticated (delete = ADMIN) |

- **Data binding**: forms bind `UserRequest` / `TaskRequest` via `th:object` / `th:field`;
  `@Valid` + `BindingResult` re-render the form with inline `th:errors` on failure.
- **CSRF** is enabled for the browser forms (Thymeleaf injects the token); the REST API
  (`/api/**`), Actuator and the H2 console are exempt and authenticate via HTTP Basic.
- Unauthenticated **page** requests redirect to `/login`; unauthenticated **API/Actuator**
  requests return `401`. UI labels are localized (English / Georgian) from the message bundles.

Packaged jar:

```bash
./mvnw clean package
java -jar target/midterm-0.0.1-SNAPSHOT.jar
```

---

## Profile configuration

Shared base lives in `application.properties`; overrides in `application-{dev,prod}.properties`.

| Profile | Database | Seed data | SQL echo | Log level (`com.example.midterm`) |
|---------|----------|-----------|----------|-----------------------------------|
| *(none)* | In-memory H2 (`jdbc:h2:mem:midterm`) | No | off | `INFO` |
| `dev`   | In-memory H2 (`jdbc:h2:mem:devdb`) | Yes (`DataInitializer`) | on | `DEBUG` |
| `prod`  | PostgreSQL (persistent) | No | off | `WARN` |

```bash
# dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
java -jar target/midterm-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# prod — DB_PASSWORD has NO default (fails fast if missing)
export DB_URL=jdbc:postgresql://localhost:5432/taskmanager
export DB_USERNAME=taskmanager
export DB_PASSWORD=yourpassword
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Externalized custom configuration (`app.settings.*`)

Bound to `config/AppSettings.java` (`@ConfigurationProperties` + `@Validated`), validated at
startup. Reflected on `GET /api/info` and used to cap list sizes (`pagination-limit`).

| Property | Type | Validation | dev / prod |
|----------|------|------------|------------|
| `app.settings.title` | String | `@NotBlank` | "Task Manager API (DEV)" / "Task Manager API" |
| `app.settings.pagination-limit` | int | `@Min(1)` | 10 / 50 |
| `app.settings.contact-email` | String | `@NotBlank @Email` | dev-team@… / support@… |
| `app.settings.registration-enabled` | boolean | — | true / false |

---

## User credentials and roles

In-memory users (BCrypt-hashed), HTTP Basic:

| Username | Password   | Roles           |
|----------|------------|-----------------|
| `user`   | `user123`  | `USER`          |
| `admin`  | `admin123` | `USER`, `ADMIN` |

**Authorization rules**

- Public: `/api/info`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`),
  `/actuator/health`, `/actuator/info`.
- Authenticated (`USER`): all other `/api/**` (CRUD on users/tasks), `/api/me`.
- `ADMIN` only: `DELETE /api/users/**`, `DELETE /api/tasks/**`, `/h2-console/**`,
  and every Actuator endpoint except health/info (metrics, prometheus, …).
  `DELETE` is also enforced at the service layer with `@PreAuthorize("hasRole('ADMIN')")`.

### Main API endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/info` | App metadata & config (i18n) | public |
| GET | `/api/me` | Current user's profile | authenticated |
| POST/GET/PUT/DELETE | `/api/users[/{id}]` | User CRUD | authenticated (DELETE = ADMIN) |
| POST/GET/PUT/DELETE | `/api/tasks[/{id}]` | Task CRUD | authenticated (DELETE = ADMIN) |

---

## Monitoring (Actuator)

Exposed at `/actuator/*` (configured in `application.properties`):

| Endpoint | Access | Purpose |
|----------|--------|---------|
| `/actuator/health` | public | Liveness; aggregates db, diskSpace, ping, ssl + **custom `users`** indicator |
| `/actuator/info` | public | App name/version + build (Maven `build-info`) + Java/OS details |
| `/actuator/metrics` | ADMIN | Micrometer metrics (incl. custom **`users.created`** counter) |
| `/actuator/prometheus` | ADMIN | Prometheus scrape format |

```bash
curl localhost:8080/actuator/health
curl localhost:8080/actuator/info
curl -u admin:admin123 localhost:8080/actuator/health          # full component details
curl -u admin:admin123 localhost:8080/actuator/metrics/users.created
curl -u admin:admin123 localhost:8080/actuator/prometheus
```

- **Custom health indicator** — `monitoring/UsersHealthIndicator` reports UP + current user
  count, DOWN if the user table is unreachable.
- **Custom metric** — `users.created` (Micrometer `Counter`) incremented on each user creation.
- Health detail visibility is `when-authorized`: anonymous callers see only `{"status":"UP"}`.

---

## Logging configuration

- **SLF4J via Lombok `@Slf4j`** in `UserService`, `TaskService`, `InfoController`,
  `DataInitializer`, `GlobalExceptionHandler`.
- **Levels:** `DEBUG` (diagnostics), `INFO` (business events), `WARN` (not-found / validation),
  `ERROR` (unexpected failures in the global handler).
- **Parameterized** only — e.g. `log.info("Created user id={}", user.getId());`.
- **Console + file**: file at `logs/app.log` (`src/main/resources/logback-spring.xml`).
- **Rolling policy** — `SizeAndTimeBasedRollingPolicy`: daily, 10 MB/file, 7 days history,
  100 MB cap (`logs/app.YYYY-MM-DD.N.log`).
- **Profile-specific levels** via `<springProfile>`: `dev` → DEBUG, `prod` → WARN, otherwise INFO.

---

## Testing

```bash
./mvnw test                 # run all tests
./mvnw clean verify         # tests + JaCoCo coverage report
```

JaCoCo HTML report: `target/site/jacoco/index.html`. Latest run: **85% instruction**,
**85% line** coverage (395/464 lines).

**99 tests**, covering positive and negative scenarios:

| Suite | Type | Scope |
|-------|------|-------|
| `service/UserServiceTest`, `service/TaskServiceTest` | **Unit** (JUnit + Mockito) | Service logic with mocked repositories; not-found paths; custom metric; **parameterized** pagination cap |
| `controller/UserControllerWebMvcTest` | **Web slice** (`@WebMvcTest` + MockMvc) | HTTP status/JSON, security (401 anon, 403 admin-only DELETE, 204 admin), 201 create, **parameterized** 400 validation, 404, 400 type-mismatch, 400 `@Min` path |
| `controller/TaskControllerWebMvcTest` | **Web slice** (`@WebMvcTest` + MockMvc) | Mirrors the user slice: 401/403/204 auth, 201 create, **parameterized** 400 validation, 404, 400 type-mismatch, 400 `@Min` path |
| `controller/AuthControllerWebMvcTest` | **Web slice** (`@WebMvcTest` + MockMvc) | `/api/me`: 401 anon, authenticated principal name + roles |
| `web/UserViewControllerTest`, `web/TaskViewControllerTest` | **Web slice** (`@WebMvcTest` + MockMvc) | Thymeleaf UI: view resolution, model binding, validation re-render with field errors, CSRF, login redirect for anon, admin-only DELETE (403/redirect) |
| `repository/UserRepositoryDataJpaTest`, `repository/TaskRepositoryDataJpaTest` | **Repository slice** (`@DataJpaTest`) | Persistence; User↔Task relationship + cascade/orphanRemoval; empty result for missing id |
| `MidtermApplicationTests`, `I18nAndConfigTests`, `DevProfileTests` | **Integration** (`@SpringBootTest`) | Full-context CRUD, auth/roles, BCrypt, i18n, config injection, dev-profile seeding |
| `ActuatorSmokeTest` | **Integration** (`@SpringBootTest`) | Actuator: public health (UP) / info, ADMIN-only metrics (403 user / 200 admin), custom `users.created` counter |

> Note: `pom.xml` sets `net.bytebuddy.experimental=true` for Surefire so Mockito can mock
> concrete classes on very new JDKs. It is a no-op on the officially supported JDK 21.

---

## Internationalization

Locale resolution is **session-first, header-fallback** (`SessionOrHeaderLocaleResolver`,
default English): the web UI's **EN / ქარ language switcher** (nav dropdown) pins a locale in
the session via `?lang=en|ka` (handled by a `LocaleChangeInterceptor`), while API clients
without a session are driven by the `Accept-Language` header. Bundles:
`messages[_en|_ka].properties` (UTF-8). Localized: UI labels, `/api/info` greeting, validation
messages, and error envelopes (404/400/500).

```bash
curl -H "Accept-Language: ka" localhost:8080/api/info                            # API: header-driven
curl -u admin:admin123 -H "Accept-Language: ka" localhost:8080/api/users/99999   # localized 404
curl "localhost:8080/?lang=ka"                                                   # UI: switcher param
```

---

## Project structure

```
config/       SecurityConfig, AppSettings, I18nConfig, OpenApiConfig, DataInitializer
controller/   UserController, TaskController, InfoController, AuthController
service/       UserService, TaskService
repository/    UserRepository, TaskRepository
entity/        User, Task
dto/           *Request / *Response
exception/     GlobalExceptionHandler, ResourceNotFoundException
monitoring/    UsersHealthIndicator   (custom Actuator health)
resources/     application*.properties, messages*.properties, logback-spring.xml
```
