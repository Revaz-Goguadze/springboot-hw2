package com.example.midterm.service;

import com.example.midterm.config.AppSettings;
import com.example.midterm.dto.TaskRequest;
import com.example.midterm.dto.TaskResponse;
import com.example.midterm.entity.Task;
import com.example.midterm.entity.User;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.repository.TaskRepository;
import com.example.midterm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AppSettings settings;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, AppSettings settings) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.settings = settings;
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        log.info("Creating task '{}' for user id={}", request.getTitle(), request.getUserId());
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.warn("Cannot create task — user id={} not found", request.getUserId());
                    return new ResourceNotFoundException("User", request.getUserId());
                });

        Task task = new Task(request.getTitle(), request.getDescription(),
                request.getCompleted(), user);
        task = taskRepository.save(task);
        log.info("Created task id={}", task.getId());
        return toResponse(task);
    }

    public List<TaskResponse> getAllTasks() {
        // app.settings.pagination-limit caps list size; the value differs per profile
        log.debug("Listing tasks, capped at paginationLimit={}", settings.getPaginationLimit());
        return taskRepository.findAll().stream()
                .limit(settings.getPaginationLimit())
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCompleted(request.getCompleted());
        task.setUser(user);
        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
        log.info("Deleted task id={}", id);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCompleted(),
                task.getUser().getId(),
                task.getUser().getName()
        );
    }
}
