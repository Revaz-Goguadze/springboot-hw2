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
 * Repository slice test ({@link DataJpaTest}) running against the in-memory H2
 * database. Verifies persistence and the Task→User relationship (positive) and
 * the empty result for a missing id (negative).
 */
@DataJpaTest
class TaskRepositoryDataJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("Persisted task is retrievable and keeps its owning user")
    void saveAndFindTask_keepsRelationship() {
        User owner = entityManager.persist(new User("Alice", "alice@example.com"));
        Task task = entityManager.persist(new Task("Write tests", "desc", false, owner));
        entityManager.flush();
        entityManager.clear();

        Optional<Task> found = taskRepository.findById(task.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Write tests");
        assertThat(found.get().getUser().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("findById returns empty for a non-existent id")
    void findById_missing_returnsEmpty() {
        assertThat(taskRepository.findById(9999L)).isEmpty();
    }
}
