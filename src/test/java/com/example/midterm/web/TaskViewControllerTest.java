package com.example.midterm.web;

import com.example.midterm.config.SecurityConfig;
import com.example.midterm.dto.TaskResponse;
import com.example.midterm.service.TaskService;
import com.example.midterm.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web slice test for the Thymeleaf {@link TaskViewController}: view resolution,
 * validation re-render (with the user dropdown re-populated) and the admin-only
 * delete rule.
 */
@WebMvcTest(TaskViewController.class)
@Import(SecurityConfig.class)
class TaskViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserService userService;

    private static TaskResponse sampleTask() {
        return new TaskResponse(1L, "Write tests", "desc", false, 1L, "Alice");
    }

    @Test
    @WithMockUser
    @DisplayName("GET /tasks renders the list view with the tasks model")
    void list_rendersList() throws Exception {
        when(taskService.getAllTasks()).thenReturn(java.util.List.of(sampleTask()));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(model().attributeExists("tasks"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /tasks/new renders a form with the user dropdown populated")
    void createForm_rendersFormWithUsers() throws Exception {
        mockMvc.perform(get("/tasks/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeExists("task", "users"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /tasks with a valid body creates and redirects to the list")
    void create_valid_redirects() throws Exception {
        when(taskService.createTask(any())).thenReturn(sampleTask());

        mockMvc.perform(post("/tasks").with(csrf())
                        .param("title", "Write tests")
                        .param("description", "desc")
                        .param("completed", "true")
                        .param("userId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));

        verify(taskService).createTask(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /tasks with an invalid body re-renders the form with field errors")
    void create_invalid_rendersFormWithErrors() throws Exception {
        mockMvc.perform(post("/tasks").with(csrf())
                        .param("title", "")
                        .param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeHasFieldErrors("task", "title", "userId"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /tasks/{id} as a non-admin is forbidden (403)")
    void delete_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(delete("/tasks/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /tasks/{id} as an admin deletes and redirects")
    void delete_asAdmin_redirects() throws Exception {
        mockMvc.perform(delete("/tasks/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));

        verify(taskService).deleteTask(1L);
    }
}
