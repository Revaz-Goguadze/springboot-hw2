package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test ({@link DataJpaTest}) for {@link UserRepository}, mirroring
 * {@link TaskRepositoryDataJpaTest}. Covers persistence (positive), the empty
 * result for a missing id (negative), and the User→Task cascade/orphanRemoval.
 */
@DataJpaTest
class UserRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Persisted user is retrievable by id")
    void saveAndFindUser() {
        User user = entityManager.persist(new User("Alice", "alice@example.com"));
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = userRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("findById returns empty for a non-existent id")
    void findById_missing_returnsEmpty() {
        assertThat(userRepository.findById(9999L)).isEmpty();
    }

    @Test
    @DisplayName("Deleting a user cascades to its tasks (orphanRemoval)")
    void deleteUser_cascadesToTasks() {
        User user = new User("Alice", "alice@example.com");
        user.getTasks().add(new Task("Homework", "do it", false, user));
        Long savedId = entityManager.persist(user).getId();
        entityManager.flush();
        entityManager.clear();
        assertThat(taskRepository.count()).isEqualTo(1);

        userRepository.deleteById(savedId);
        entityManager.flush();

        assertThat(userRepository.findById(savedId)).isEmpty();
        assertThat(taskRepository.count()).isZero();
    }
}
