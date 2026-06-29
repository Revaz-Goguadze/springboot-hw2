package com.example.midterm.service;

import com.example.midterm.config.AppSettings;
import com.example.midterm.dto.UserRequest;
import com.example.midterm.dto.UserResponse;
import com.example.midterm.entity.User;
import com.example.midterm.exception.ResourceNotFoundException;
import com.example.midterm.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserService} — repository is mocked (Mockito), no
 * Spring context. Covers positive paths, negative (not-found) paths, the custom
 * users.created metric, and the profile-driven pagination cap.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppSettings settings;

    private SimpleMeterRegistry meterRegistry;
    private UserService userService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        userService = new UserService(userRepository, settings, meterRegistry);
    }

    @Test
    @DisplayName("createUser persists the user and increments the users.created counter")
    void createUser_savesAndCountsMetric() {
        User saved = new User("Alice", "alice@example.com");
        saved.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.createUser(new UserRequest("Alice", "alice@example.com"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(meterRegistry.counter("users.created").count()).isEqualTo(1.0);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getUserById returns the mapped user when present")
    void getUserById_found() {
        User user = new User("Bob", "bob@example.com");
        user.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException for a missing id")
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("deleteUser throws and never deletes when the id does not exist")
    void deleteUser_notFound() {
        when(userRepository.existsById(123L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(123L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @ParameterizedTest(name = "paginationLimit={0} caps a 5-row table")
    @ValueSource(ints = {1, 2, 5})
    @DisplayName("getAllUsers never returns more than the configured pagination limit")
    void getAllUsers_respectsPaginationLimit(int limit) {
        // DB returns at most `limit` rows because the cap is pushed down as SQL LIMIT.
        List<User> page = IntStream.range(0, limit)
                .mapToObj(i -> new User("U" + i, "u" + i + "@example.com"))
                .toList();
        when(settings.getPaginationLimit()).thenReturn(limit);
        when(userRepository.findAll(PageRequest.of(0, limit))).thenReturn(new PageImpl<>(page));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(limit);
        // Verify the service asks the DB for exactly `limit` rows, not the whole table.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(limit);
    }
}
