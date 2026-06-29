package com.example.taskmanager.service;

import com.example.taskmanager.config.AppSettings;
import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link TaskService} with mocked repositories. Covers the
 * happy path plus the two not-found negative paths (missing owner on create,
 * missing task on lookup).
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppSettings settings;

    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("createTask saves the task for an existing owner")
    void createTask_ownerExists() {
        User owner = new User("Alice", "alice@example.com");
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TaskResponse response = taskService.createTask(
                new TaskRequest("Write tests", "desc", false, 1L));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Write tests");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createTask throws when the owner user does not exist")
    void createTask_ownerMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(
                new TaskRequest("Orphan", "desc", false, 404L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 404");
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTaskById throws for a missing task id")
    void getTaskById_notFound() {
        when(taskRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(55L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 55");
    }
}
