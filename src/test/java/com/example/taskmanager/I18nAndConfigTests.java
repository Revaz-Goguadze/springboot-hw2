package com.example.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers HW2 features: Accept-Language driven i18n (welcome message, 404 payloads,
 * localized validation errors) and the app.settings.pagination-limit cap
 * (overridden to 2 here so the cap is observable with little data).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.settings.pagination-limit=2")
@Sql(statements = {"DELETE FROM tasks", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class I18nAndConfigTests {

    @Autowired
    private TestRestTemplate rest;

    private TestRestTemplate adminRest() {
        return rest.withBasicAuth("admin", "admin123");
    }

    private HttpHeaders georgian() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ka");
        return headers;
    }

    // --- i18n: Accept-Language drives the response language ---

    @Test
    void infoEndpointReturnsEnglishByDefault() {
        ResponseEntity<Map> response = rest.getForEntity("/api/info", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("message")).startsWith("Welcome to");
    }

    @Test
    void infoEndpointReturnsGeorgianForAcceptLanguageKa() {
        ResponseEntity<Map> response = rest.exchange("/api/info", HttpMethod.GET,
                new HttpEntity<>(georgian()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("message"))
                .contains("კეთილი იყოს თქვენი მობრძანება");
    }

    @Test
    void notFoundPayloadIsLocalizedToGeorgian() {
        ResponseEntity<Map> response = adminRest().exchange("/api/users/99999", HttpMethod.GET,
                new HttpEntity<>(georgian()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("ვერ მოიძებნა");
        assertThat((String) response.getBody().get("message")).contains("ვერ მოიძებნა id-ით: 99999");
    }

    @Test
    void validationErrorsAreLocalizedToGeorgian() {
        HttpHeaders headers = georgian();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of("name", "", "email", "not-an-email");

        ResponseEntity<Map> response = adminRest().exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("ვალიდაცია ვერ შესრულდა");
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        // a blank name violates both @NotBlank and @Size; either localized message may win
        assertThat(fieldErrors.get("name"))
                .isIn("სახელის მითითება სავალდებულოა", "სახელი უნდა იყოს 2-დან 100 სიმბოლომდე");
        assertThat(fieldErrors.get("email")).isEqualTo("ელფოსტა უნდა იყოს ვალიდური");
    }

    @Test
    void validationErrorsAreEnglishByDefault() {
        var body = Map.of("name", "", "email", "not-an-email");
        ResponseEntity<Map> response = adminRest().postForEntity("/api/users", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        // a blank name violates both @NotBlank and @Size; either localized message may win
        assertThat(fieldErrors.get("name"))
                .isIn("Name is required", "Name must be between 2 and 100 characters");
        assertThat(fieldErrors.get("email")).isEqualTo("Email must be valid");
    }

    // --- Custom config: pagination-limit caps list endpoints ---

    @Test
    void userListIsCappedAtPaginationLimit() {
        adminRest().postForEntity("/api/users", Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        adminRest().postForEntity("/api/users", Map.of("name", "Bob", "email", "b@x.com"), Map.class);
        adminRest().postForEntity("/api/users", Map.of("name", "Carol", "email", "c@x.com"), Map.class);

        ResponseEntity<List> response = adminRest().getForEntity("/api/users", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void infoEndpointExposesConfiguredPaginationLimit() {
        ResponseEntity<Map> response = rest.getForEntity("/api/info", Map.class);
        assertThat(response.getBody().get("paginationLimit")).isEqualTo(2);
    }

    // --- i18n: error/validation messages externalized to the bundles are localized ---

    @Test
    void typeMismatchMessageIsLocalizedToGeorgian() {
        // /api/users/{id} with a non-numeric id -> MethodArgumentTypeMismatch -> error.badrequest.param
        ResponseEntity<Map> response = adminRest().exchange("/api/users/not-a-number", HttpMethod.GET,
                new HttpEntity<>(georgian()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).contains("მნიშვნელობა არასწორია");
    }

    @Test
    void taskValidationMessageIsLocalizedToGeorgian() {
        HttpHeaders headers = georgian();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // omit userId -> @NotNull{task.userId.required}
        var body = Map.of("title", "Valid title", "completed", true);

        ResponseEntity<Map> response = adminRest().exchange("/api/tasks", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().get("fieldErrors");
        assertThat(fieldErrors.get("userId")).isEqualTo("მომხმარებლის ID სავალდებულოა");
    }

    // --- UI language switcher: ?lang=ka pins the session locale and renders Georgian ---

    @Test
    void uiLanguageSwitcherRendersGeorgian() {
        // The home page nav is localized; ?lang=ka must flip it to Georgian in the same request.
        ResponseEntity<String> response = rest.getForEntity("/?lang=ka", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("მთავარი"); // nav.home (ka)
    }

    @Test
    void uiDefaultsToEnglishWithoutLangParam() {
        ResponseEntity<String> response = rest.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(">Home<");
    }
}
