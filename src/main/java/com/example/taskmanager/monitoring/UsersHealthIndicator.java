package com.example.taskmanager.monitoring;

import com.example.taskmanager.repository.UserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator health indicator. Contributes a {@code users} component to
 * {@code /actuator/health}: UP (with the current row count) when the user table
 * is reachable, DOWN when the query fails. Demonstrates application-aware health
 * beyond the built-in datasource check.
 */
@Component("users")
public class UsersHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    public UsersHealthIndicator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Health health() {
        try {
            long count = userRepository.count();
            return Health.up()
                    .withDetail("userCount", count)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
