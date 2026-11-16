package com.tuneacademy.backend.tutoring;

import org.springframework.data.jpa.repository.JpaRepository;

// The id is the request's id, so existsById(requestId) asks "was this request turned into an engagement?"
public interface EngagementRepository extends JpaRepository<Engagement, Long> {
}
