package com.example.midterm.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * Internationalization wiring.
 *
 * <p>Locale resolution is session-first, header-fallback: the UI language
 * switcher pins a locale in the session via {@code ?lang=en|ka}, while API
 * clients without a session continue to be driven by the {@code Accept-Language}
 * header. English is the default fallback.
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        SessionOrHeaderLocaleResolver resolver = new SessionOrHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.of("ka")));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /** Switches the session locale when a request carries {@code ?lang=en|ka}. */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * Routes Bean Validation message interpolation (the {@code {key}} syntax in
     * constraint annotations) through the application {@link MessageSource}, so
     * validation errors are localized from the same message bundles.
     */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
