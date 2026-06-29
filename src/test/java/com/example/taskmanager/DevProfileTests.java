package com.example.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application under the dev profile and asserts its profile-specific
 * behavior: DataInitializer seed data and the dev values of AppSettings.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class DevProfileTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void devProfileSeedsTestData() {
        ResponseEntity<List> response = rest.withBasicAuth("admin", "admin123")
                .getForEntity("/api/users", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2); // Alice and Bob from DataInitializer
    }

    @Test
    void devProfileExposesDevConfiguration() {
        ResponseEntity<Map> response = rest.getForEntity("/api/info", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("title")).isEqualTo("Task Manager API (DEV)");
        assertThat(response.getBody().get("paginationLimit")).isEqualTo(10);
        assertThat((List<String>) response.getBody().get("activeProfiles")).containsExactly("dev");
    }
}
