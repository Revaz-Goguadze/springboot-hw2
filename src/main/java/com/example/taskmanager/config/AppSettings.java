package com.example.taskmanager.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized, type-safe configuration bound from the {@code app.settings.*} keys.
 * Values are supplied per-profile (see application-dev/application-prod) and validated
 * at startup via JSR-303 constraints together with {@link Validated}.
 */
@ConfigurationProperties(prefix = "app.settings")
@Validated
public class AppSettings {

    /** Human-readable application title exposed on metadata endpoints. */
    @NotBlank
    private String title;

    /** Maximum number of items a list endpoint may return per page. */
    @Min(1)
    private int paginationLimit;

    /** Support contact address shown to API consumers. */
    @NotBlank
    @Email
    private String contactEmail;

    /** Feature flag toggling whether self-service user registration is advertised. */
    private boolean registrationEnabled;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPaginationLimit() {
        return paginationLimit;
    }

    public void setPaginationLimit(int paginationLimit) {
        this.paginationLimit = paginationLimit;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }
}
