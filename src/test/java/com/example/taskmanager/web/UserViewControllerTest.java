package com.example.taskmanager.web;

import com.example.taskmanager.config.SecurityConfig;
import com.example.taskmanager.dto.UserResponse;
import com.example.taskmanager.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web slice test for the Thymeleaf {@link UserViewController}: verifies view
 * resolution, model binding, validation re-render, CSRF and the admin-only
 * delete rule. The real {@link SecurityConfig} is imported.
 */
@WebMvcTest(UserViewController.class)
@Import(SecurityConfig.class)
class UserViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /users unauthenticated redirects to the login page")
    void list_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /users renders the list view with the users model")
    void list_authenticated_rendersList() throws Exception {
        when(userService.getAllUsers()).thenReturn(java.util.List.of(
                new UserResponse(1L, "Alice", "alice@example.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /users/new renders an empty form")
    void createForm_rendersForm() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users with a valid body creates and redirects to the list")
    void create_valid_redirects() throws Exception {
        when(userService.createUser(any())).thenReturn(new UserResponse(1L, "Alice", "alice@example.com"));

        mockMvc.perform(post("/users").with(csrf())
                        .param("name", "Alice")
                        .param("email", "alice@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        verify(userService).createUser(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /users with an invalid body re-renders the form with field errors")
    void create_invalid_rendersFormWithErrors() throws Exception {
        mockMvc.perform(post("/users").with(csrf())
                        .param("name", "")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeHasFieldErrors("user", "name", "email"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /users/{id} as a non-admin is forbidden (403)")
    void delete_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(delete("/users/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /users/{id} as an admin deletes and redirects")
    void delete_asAdmin_redirects() throws Exception {
        mockMvc.perform(delete("/users/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        verify(userService).deleteUser(1L);
    }
}
