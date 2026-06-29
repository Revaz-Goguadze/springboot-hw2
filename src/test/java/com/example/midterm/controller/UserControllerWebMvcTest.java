package com.example.midterm.controller;

import com.example.midterm.config.SecurityConfig;
import com.example.midterm.dto.UserResponse;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer slice test for {@link UserController} using {@link WebMvcTest} +
 * MockMvc. The service is mocked; the real {@link SecurityConfig} is imported so
 * authentication/authorization and the global exception handler are exercised.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users without credentials is rejected with 401")
    void getUsers_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users authenticated returns 200")
    void getUsers_authenticated_returnsOk() throws Exception {
        when(userService.getAllUsers()).thenReturn(java.util.List.of(
                new UserResponse(1L, "Alice", "alice@example.com")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/users with a valid body returns 201 Created")
    void createUser_valid_returnsCreated() throws Exception {
        when(userService.createUser(any())).thenReturn(
                new UserResponse(1L, "Alice", "alice@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest(name = "invalid body #{index} -> 400")
    @WithMockUser
    @ValueSource(strings = {
            "{\"name\":\"\",\"email\":\"alice@example.com\"}",   // blank name
            "{\"name\":\"A\",\"email\":\"alice@example.com\"}",  // name too short
            "{\"name\":\"Alice\",\"email\":\"not-an-email\"}",   // bad email
            "{\"name\":\"Alice\",\"email\":\"\"}"                 // blank email
    })
    @DisplayName("POST /api/users with an invalid body returns 400 with field errors")
    void createUser_invalid_returnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/{id} for a missing user returns 404")
    void getUser_missing_returnsNotFound() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User", 99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/{id} with a non-numeric id returns 400, not 500")
    void getUser_typeMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    @DisplayName("Unmapped route returns 404, not 500")
    void unknownRoute_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/{id} with a non-positive id returns 400 (@Min path validation)")
    void getUser_idBelowOne_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/users/{id} as a non-admin is forbidden (403)")
    void deleteUser_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/users/{id} as an admin returns 204 No Content")
    void deleteUser_asAdmin_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
        verify(userService).deleteUser(1L);
    }
}
