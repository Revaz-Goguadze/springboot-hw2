package com.example.midterm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Actuator monitoring surface is not just configured but functional
 * and correctly secured: health/info are public, metrics is ADMIN-only, and the
 * custom {@code users.created} Micrometer counter is registered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorSmokeTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("/actuator/health is public and reports UP")
    void healthIsPublicAndReportsUp() {
        ResponseEntity<Map> response = rest.getForEntity("/actuator/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("/actuator/info is public")
    void infoIsPublic() {
        ResponseEntity<Map> response = rest.getForEntity("/actuator/info", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("/actuator/metrics is forbidden for a non-admin user")
    void metricsForbiddenForNonAdmin() {
        ResponseEntity<Map> response = rest.withBasicAuth("user", "user123")
                .getForEntity("/actuator/metrics", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("/actuator/metrics is accessible to an admin")
    void metricsAccessibleToAdmin() {
        ResponseEntity<Map> response = rest.withBasicAuth("admin", "admin123")
                .getForEntity("/actuator/metrics", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("names");
    }

    @Test
    @DisplayName("Custom users.created counter is exposed via /actuator/metrics")
    void customUsersCreatedMetricIsRegistered() {
        ResponseEntity<Map> response = rest.withBasicAuth("admin", "admin123")
                .getForEntity("/actuator/metrics/users.created", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("users.created");
    }
}
