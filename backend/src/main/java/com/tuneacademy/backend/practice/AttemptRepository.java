package com.tuneacademy.backend.practice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    // Owner-only read: the user's ID is part of the lookup, so someone else's attempt is simply "not found".
    Optional<Attempt> findByIdAndUserId(Long id, Long userId);

    // "My past scores" on an exercise page, newest first.
    List<Attempt> findByUserIdAndExerciseIdOrderByCreatedAtDesc(Long userId, Long exerciseId);
}
