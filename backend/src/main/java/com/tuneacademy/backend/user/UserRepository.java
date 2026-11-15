package com.tuneacademy.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring writes the code behind this interface when the app starts.
// JpaRepository<User, Long> = "a repository for User rows, whose id is a Long".
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring reads the method name and builds: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Same idea, but only answers yes/no. Signup uses it to reject an email that's taken.
    boolean existsByEmail(String email);
}
