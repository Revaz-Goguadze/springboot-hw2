package com.example.midterm.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the landing page and the custom login page for the Thymeleaf UI.
 * The REST API lives under {@code /api/**} and is unaffected by these views.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
