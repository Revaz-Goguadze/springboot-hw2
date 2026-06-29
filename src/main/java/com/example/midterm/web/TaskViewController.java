package com.example.midterm.web;

import com.example.midterm.dto.TaskRequest;
import com.example.midterm.dto.TaskResponse;
import com.example.midterm.service.TaskService;
import com.example.midterm.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Thymeleaf UI for Task CRUD. Mirrors {@link UserViewController}; the create/edit
 * form binds the existing {@link TaskRequest} DTO and renders a user dropdown so
 * the owning user is selected by id.
 */
@Slf4j
@Controller
@RequestMapping("/tasks")
public class TaskViewController {

    private final TaskService taskService;
    private final UserService userService;

    public TaskViewController(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tasks", taskService.getAllTasks());
        return "tasks/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("task", new TaskRequest());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/tasks");
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("task") TaskRequest task, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("editing", false);
            model.addAttribute("formAction", "/tasks");
            return "tasks/form";
        }
        taskService.createTask(task);
        return "redirect:/tasks";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TaskResponse existing = taskService.getTaskById(id);
        model.addAttribute("task", new TaskRequest(
                existing.getTitle(), existing.getDescription(), existing.getCompleted(), existing.getUserId()));
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/tasks/" + id);
        return "tasks/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("task") TaskRequest task,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("editing", true);
            model.addAttribute("formAction", "/tasks/" + id);
            return "tasks/form";
        }
        taskService.updateTask(id, task);
        return "redirect:/tasks";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return "redirect:/tasks";
    }
}
