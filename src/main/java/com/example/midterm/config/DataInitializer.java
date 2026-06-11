package com.example.midterm.config;

import com.example.midterm.entity.Task;
import com.example.midterm.entity.User;
import com.example.midterm.repository.TaskRepository;
import com.example.midterm.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory H2 database with sample data. Active only under the
 * {@code dev} profile so production never runs against generated test data.
 */
@Slf4j
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public DataInitializer(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.debug("Dev data already present ({} users) — skipping seed", userRepository.count());
            return;
        }

        log.info("Seeding dev profile test data into H2");

        User alice = userRepository.save(new User("Alice", "alice@example.com"));
        User bob = userRepository.save(new User("Bob", "bob@example.com"));

        taskRepository.save(new Task("Write report", "Quarterly summary", false, alice));
        taskRepository.save(new Task("Review PR", "Approve teammate changes", true, alice));
        taskRepository.save(new Task("Plan sprint", "Backlog grooming", false, bob));

        log.info("Seeded {} users and {} tasks", userRepository.count(), taskRepository.count());
    }
}
