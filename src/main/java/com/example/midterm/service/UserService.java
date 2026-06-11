package com.example.midterm.service;

import com.example.midterm.config.AppSettings;
import com.example.midterm.dto.UserRequest;
import com.example.midterm.dto.UserResponse;
import com.example.midterm.entity.User;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
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

    public UserService(UserRepository userRepository, AppSettings settings) {
        this.userRepository = userRepository;
        this.settings = settings;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with email '{}'", request.getEmail());
        User user = new User(request.getName(), request.getEmail());
        user = userRepository.save(user);
        log.info("Created user id={}", user.getId());
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        // app.settings.pagination-limit caps list size; the value differs per profile
        log.debug("Listing users, capped at paginationLimit={}", settings.getPaginationLimit());
        return userRepository.findAll().stream()
                .limit(settings.getPaginationLimit())
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
