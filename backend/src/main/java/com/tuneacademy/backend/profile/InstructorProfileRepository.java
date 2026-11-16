package com.tuneacademy.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

// The id is the instructor's user id, so findById(userId) finds an instructor's profile.
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {
}
