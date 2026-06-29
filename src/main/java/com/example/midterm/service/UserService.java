package com.example.midterm.service;

import com.example.midterm.config.AppSettings;
import com.example.midterm.dto.UserRequest;
import com.example.midterm.dto.UserResponse;
import com.example.midterm.entity.User;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AppSettings settings;
    /** Custom Micrometer metric, scrapeable at /actuator/metrics/users.created. */
    private final Counter usersCreatedCounter;

    public UserService(UserRepository userRepository, AppSettings settings, MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.settings = settings;
        this.usersCreatedCounter = Counter.builder("users.created")
                .description("Total number of users created")
                .register(meterRegistry);
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with email '{}'", request.getEmail());
        User user = new User(request.getName(), request.getEmail());
        user = userRepository.save(user);
        usersCreatedCounter.increment();
        log.info("Created user id={}", user.getId());
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        // app.settings.pagination-limit caps list size; the value differs per profile.
        // The cap is pushed to the database (SQL LIMIT via Pageable) rather than
        // loading the whole table and trimming in memory.
        int limit = settings.getPaginationLimit();
        log.debug("Listing users, capped at paginationLimit={}", limit);
        return userRepository.findAll(PageRequest.of(0, limit)).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        log.debug("Fetching user id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User id={} not found", id);
                    return new ResourceNotFoundException("User", id);
                });
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
