package com.example.midterm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(statements = {"DELETE FROM tasks", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MidtermApplicationTests {

    @Autowired
    private TestRestTemplate rest;

    @LocalServerPort
    private int port;

    @Autowired
    private com.example.midterm.service.TaskService taskService;

    @Autowired
    private com.example.midterm.service.UserService userService;

    @Autowired
    private com.example.midterm.repository.TaskRepository taskRepository;

    @Autowired
    private com.example.midterm.repository.UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestRestTemplate adminRest() {
        return rest.withBasicAuth("admin", "admin123");
    }

    private TestRestTemplate userRest() {
        return rest.withBasicAuth("user", "user123");
    }

    // --- User CRUD ---

    @Test
    void createUser() {
        var user = Map.of("name", "Alice", "email", "alice@example.com");
        ResponseEntity<Map> response = adminRest().postForEntity("/api/users", user, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "name", "email");
        assertThat(response.getBody().get("name")).isEqualTo("Alice");
    }

    @Test
    void getAllUsers() {
        adminRest().postForEntity("/api/users", Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        adminRest().postForEntity("/api/users", Map.of("name", "Bob", "email", "b@x.com"), Map.class);

        ResponseEntity<List> response = adminRest().getForEntity("/api/users", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getUserById() {
        var created = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "alice@x.com"), Map.class);
        Long id = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map> response = adminRest().getForEntity("/api/users/" + id, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Alice");
    }

    @Test
    void updateUser() {
        var created = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "alice@x.com"), Map.class);
        Long id = ((Number) created.getBody().get("id")).longValue();

        adminRest().put("/api/users/" + id, Map.of("name", "Alice Updated", "email", "alice@x.com"));
        ResponseEntity<Map> response = adminRest().getForEntity("/api/users/" + id, Map.class);
        assertThat(response.getBody().get("name")).isEqualTo("Alice Updated");
    }

    @Test
    void deleteUser() {
        var created = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "alice@x.com"), Map.class);
        Long id = ((Number) created.getBody().get("id")).longValue();

        adminRest().delete("/api/users/" + id);
        ResponseEntity<Map> response = adminRest().getForEntity("/api/users/" + id, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Task CRUD ---

    @Test
    void createTask() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var task = Map.of("title", "Homework", "description", "Do it", "completed", false, "userId", userId);
        ResponseEntity<Map> response = adminRest().postForEntity("/api/tasks", task, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "title", "completed", "userId", "userName");
        assertThat(response.getBody().get("title")).isEqualTo("Homework");
        assertThat(response.getBody().get("userName")).isEqualTo("Alice");
    }

    @Test
    void getAllTasks() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        adminRest().postForEntity("/api/tasks",
                Map.of("title", "Task One", "description", "d1", "completed", false, "userId", userId), Map.class);
        adminRest().postForEntity("/api/tasks",
                Map.of("title", "Task Two", "description", "d2", "completed", true, "userId", userId), Map.class);

        ResponseEntity<List> response = adminRest().getForEntity("/api/tasks", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getTaskById() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var created = adminRest().postForEntity("/api/tasks",
                Map.of("title", "Homework", "description", "Do it", "completed", false, "userId", userId), Map.class);
        Long taskId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map> response = adminRest().getForEntity("/api/tasks/" + taskId, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("title")).isEqualTo("Homework");
    }

    @Test
    void updateTask() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var created = adminRest().postForEntity("/api/tasks",
                Map.of("title", "Homework", "description", "Do it", "completed", false, "userId", userId), Map.class);
        Long taskId = ((Number) created.getBody().get("id")).longValue();

        adminRest().put("/api/tasks/" + taskId,
                Map.of("title", "Homework", "description", "Do it", "completed", true, "userId", userId));
        ResponseEntity<Map> response = adminRest().getForEntity("/api/tasks/" + taskId, Map.class);
        assertThat(response.getBody().get("completed")).isEqualTo(true);
    }

    @Test
    void deleteTask() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var created = adminRest().postForEntity("/api/tasks",
                Map.of("title", "Homework", "description", "Do it", "completed", false, "userId", userId), Map.class);
        Long taskId = ((Number) created.getBody().get("id")).longValue();

        adminRest().delete("/api/tasks/" + taskId);
        ResponseEntity<Map> response = adminRest().getForEntity("/api/tasks/" + taskId, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Validation & Error Handling ---

    @Test
    void invalidEmailReturns400() {
        var badUser = Map.of("name", "Alice", "email", "not-an-email");
        ResponseEntity<Map> response = adminRest().postForEntity("/api/users", badUser, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("fieldErrors");
    }

    @Test
    void blankNameReturns400() {
        var badUser = Map.of("name", "", "email", "a@x.com");
        ResponseEntity<Map> response = adminRest().postForEntity("/api/users", badUser, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingUserIdReturns404() {
        ResponseEntity<Map> response = adminRest().getForEntity("/api/users/99999", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message").toString()).contains("User not found");
    }

    @Test
    void taskWithoutUserReturns404() {
        var task = Map.of("title", "Test", "description", "d", "completed", false, "userId", 99999);
        ResponseEntity<Map> response = adminRest().postForEntity("/api/tasks", task, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void blankTaskTitleReturns400() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var badTask = Map.of("title", "", "description", "d", "completed", false, "userId", userId);
        ResponseEntity<Map> response = adminRest().postForEntity("/api/tasks", badTask, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateEmailAllowed() {
        var u1 = Map.of("name", "User One", "email", "same@x.com");
        var u2 = Map.of("name", "User Two", "email", "same@x.com");

        ResponseEntity<Map> r1 = adminRest().postForEntity("/api/users", u1, Map.class);
        ResponseEntity<Map> r2 = adminRest().postForEntity("/api/users", u2, Map.class);

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // --- Security ---

    @Test
    void unauthenticatedApiRequestsAreRejected() throws Exception {
        var url = URI.create("http://localhost:" + port + "/api/tasks").toURL();
        var connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);

        assertThat(connection.getResponseCode()).isIn(302, 401);
    }

    @Test
    void authenticatedUserCanReadTasks() {
        ResponseEntity<List> response = userRest().getForEntity("/api/tasks", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthenticatedApiMeRequestsAreRejected() throws Exception {
        var url = URI.create("http://localhost:" + port + "/api/me").toURL();
        var connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);

        assertThat(connection.getResponseCode()).isIn(302, 401);
    }

    @Test
    void authenticatedApiMeReturnsCurrentUser() {
        ResponseEntity<Map> response = userRest().getForEntity("/api/me", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("username")).isEqualTo("user");
        assertThat(response.getBody().get("roles")).isEqualTo(List.of("USER"));
    }

    @Test
    void userRoleCannotAccessH2Console() {
        ResponseEntity<String> response = userRest().getForEntity("/h2-console/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminRoleCanAccessH2Console() {
        ResponseEntity<String> response = adminRest().getForEntity("/h2-console/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userRoleCannotDeleteTasks() {
        var user = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "a@x.com"), Map.class);
        Long userId = ((Number) user.getBody().get("id")).longValue();

        var created = adminRest().postForEntity("/api/tasks",
                Map.of("title", "Homework", "description", "Do it", "completed", false, "userId", userId), Map.class);
        Long taskId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Void> response = userRest()
                .exchange("/api/tasks/" + taskId, HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void userRoleCannotDeleteUsers() {
        var created = adminRest().postForEntity("/api/users",
                Map.of("name", "Alice", "email", "alice@x.com"), Map.class);
        Long userId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Void> response = userRest()
                .exchange("/api/users/" + userId, HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void configuredUsersUseBCryptPasswords() {
        var admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.getPassword()).startsWith("$2");
        assertThat(admin.getPassword()).doesNotContain("admin123");
        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(roles = "USER")
    void methodSecurityBlocksUserRoleFromDeletingTasks() {
        var user = userRepository.save(new com.example.midterm.entity.User("Alice", "a@x.com"));
        var task = taskRepository.save(new com.example.midterm.entity.Task("Homework", "Do it", false, user));

        assertThatThrownBy(() -> taskService.deleteTask(task.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "USER")
    void methodSecurityBlocksUserRoleFromDeletingUsers() {
        var user = userRepository.save(new com.example.midterm.entity.User("Alice", "a@x.com"));

        assertThatThrownBy(() -> userService.deleteUser(user.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void methodSecurityAllowsAdminRoleToDeleteTasks() {
        var user = userRepository.save(new com.example.midterm.entity.User("Alice", "a@x.com"));
        var task = taskRepository.save(new com.example.midterm.entity.Task("Homework", "Do it", false, user));

        taskService.deleteTask(task.getId());

        assertThat(taskRepository.existsById(task.getId())).isFalse();
    }
}
