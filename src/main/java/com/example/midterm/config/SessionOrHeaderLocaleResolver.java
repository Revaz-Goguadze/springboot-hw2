package com.example.midterm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Locale resolver that prefers a user-chosen locale stored in the HTTP session
 * (set by the UI language switcher via {@code ?lang=}), and otherwise falls back
 * to the request's {@code Accept-Language} header.
 *
 * <p>This keeps the existing API behavior intact — clients that send
 * {@code Accept-Language: ka} still get Georgian responses, since no session
 * locale is set for them — while letting the browser UI pin a language.
 */
public class SessionOrHeaderLocaleResolver extends AcceptHeaderLocaleResolver {

    static final String LOCALE_SESSION_ATTR = "UI_LOCALE";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LOCALE_SESSION_ATTR) instanceof Locale locale) {
            return locale;
        }
        return super.resolveLocale(request);
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        request.getSession(true).setAttribute(LOCALE_SESSION_ATTR, locale);
    }
}
