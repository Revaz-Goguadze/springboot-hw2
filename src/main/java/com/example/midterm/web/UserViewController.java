package com.example.midterm.web;

import com.example.midterm.dto.UserRequest;
import com.example.midterm.dto.UserResponse;
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
 * Thymeleaf UI for User CRUD. Reuses {@link UserService} and the existing
 * {@link UserRequest} DTO as the form-backing object, so server-side data
 * binding ({@code th:object}/{@code th:field}) and Bean Validation
 * ({@code @Valid} + {@link BindingResult}) drive the forms.
 */
@Slf4j
@Controller
@RequestMapping("/users")
public class UserViewController {

    private final UserService userService;

    public UserViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", new UserRequest());
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/users");
        return "users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("user") UserRequest user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("formAction", "/users");
            return "users/form";
        }
        userService.createUser(user);
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserResponse existing = userService.getUserById(id);
        model.addAttribute("user", new UserRequest(existing.getName(), existing.getEmail()));
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/users/" + id);
        return "users/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("user") UserRequest user,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("formAction", "/users/" + id);
            return "users/form";
        }
        userService.updateUser(id, user);
        return "redirect:/users";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}
