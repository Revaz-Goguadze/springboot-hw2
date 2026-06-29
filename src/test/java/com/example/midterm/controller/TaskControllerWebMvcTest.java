package com.example.midterm.controller;

import com.example.midterm.config.SecurityConfig;
import com.example.midterm.dto.TaskResponse;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.service.TaskService;
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
 * Web layer slice test for {@link TaskController} using {@link WebMvcTest} +
 * MockMvc. Mirrors {@link UserControllerWebMvcTest}: the service is mocked and
 * the real {@link SecurityConfig} is imported so authentication, authorization
 * (admin-only DELETE) and the global exception handler are exercised.
 */
@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    private static TaskResponse sampleTask() {
        return new TaskResponse(1L, "Write tests", "desc", false, 1L, "Alice");
    }

    @Test
    @DisplayName("GET /api/tasks without credentials is rejected with 401")
    void getTasks_unauthenticated_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks authenticated returns 200")
    void getTasks_authenticated_returnsOk() throws Exception {
        when(taskService.getAllTasks()).thenReturn(java.util.List.of(sampleTask()));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write tests"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/tasks with a valid body returns 201 Created")
    void createTask_valid_returnsCreated() throws Exception {
        when(taskService.createTask(any())).thenReturn(sampleTask());

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Write tests\",\"description\":\"desc\",\"completed\":false,\"userId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @ParameterizedTest(name = "invalid body #{index} -> 400")
    @WithMockUser
    @ValueSource(strings = {
            "{\"title\":\"\",\"completed\":false,\"userId\":1}",          // blank title
            "{\"title\":\"A\",\"completed\":false,\"userId\":1}",         // title too short
            "{\"title\":\"Valid title\",\"completed\":null,\"userId\":1}",// completed null
            "{\"title\":\"Valid title\",\"completed\":false,\"userId\":null}" // userId null
    })
    @DisplayName("POST /api/tasks with an invalid body returns 400 with field errors")
    void createTask_invalid_returnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} for a missing task returns 404")
    void getTask_missing_returnsNotFound() throws Exception {
        when(taskService.getTaskById(99L))
                .thenThrow(new ResourceNotFoundException("Task", 99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} with a non-numeric id returns 400, not 500")
    void getTask_typeMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} with a non-positive id returns 400 (@Min path validation)")
    void getTask_idBelowOne_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/tasks/{id} as a non-admin is forbidden (403)")
    void deleteTask_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/tasks/{id} as an admin returns 204 No Content")
    void deleteTask_asAdmin_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
        verify(taskService).deleteTask(1L);
    }
}
