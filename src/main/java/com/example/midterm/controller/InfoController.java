package com.example.midterm.controller;

import com.example.midterm.config.AppSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Public metadata endpoint. Demonstrates injecting the externalized
 * {@link AppSettings} configuration and the {@link MessageSource} to build a
 * response whose greeting is localized from the request's {@code Accept-Language}.
 */
@Slf4j
@RestController
@RequestMapping("/api/info")
@Tag(name = "Info", description = "Application metadata and configuration")
public class InfoController {

    private final AppSettings settings;
    private final MessageSource messageSource;
    private final Environment environment;

    public InfoController(AppSettings settings, MessageSource messageSource, Environment environment) {
        this.settings = settings;
        this.messageSource = messageSource;
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Get application metadata and configuration")
    public Map<String, Object> info() {
        Locale locale = LocaleContextHolder.getLocale();
        String greeting = messageSource.getMessage("app.welcome",
                new Object[]{settings.getTitle()}, locale);

        log.info("Serving /api/info — locale={}, activeProfiles={}",
                locale.getLanguage(), (Object) environment.getActiveProfiles());
        log.debug("Resolved configuration: title='{}', paginationLimit={}, contactEmail='{}'",
                settings.getTitle(), settings.getPaginationLimit(), settings.getContactEmail());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", greeting);
        body.put("title", settings.getTitle());
        body.put("paginationLimit", settings.getPaginationLimit());
        body.put("contactEmail", settings.getContactEmail());
        body.put("registrationEnabled", settings.isRegistrationEnabled());
        body.put("activeProfiles", environment.getActiveProfiles());
        return body;
    }
}
