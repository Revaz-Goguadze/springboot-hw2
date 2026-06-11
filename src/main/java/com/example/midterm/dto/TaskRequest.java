package com.example.midterm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskRequest {

    @NotBlank(message = "{task.title.required}")
    @Size(min = 2, max = 150, message = "{task.title.size}")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Completed flag is required")
    private Boolean completed;

    @NotNull(message = "User ID is required")
    private Long userId;

    public TaskRequest() {
    }

    public TaskRequest(String title, String description, Boolean completed, Long userId) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
