package com.tuneacademy.backend.tutoring;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A learner asking an instructor for lessons, with a short message. The instructor accepts or declines it.
 * The table's session_type, weekly_slots and weeks columns aren't mapped: those features are cut,
 * so they keep their defaults (or stay empty).
 */
@Entity
@Table(name = "tutoring_requests")
public class TutoringRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long learnerId;

    private Long instructorId;

    private String message;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private Instant createdAt = Instant.now();

    private Instant respondedAt; // empty until the instructor answers

    protected TutoringRequest() {
    }

    public TutoringRequest(Long learnerId, Long instructorId, String message) {
        this.learnerId = learnerId;
        this.instructorId = instructorId;
        this.message = message;
    }

    /** The instructor's answer: ACCEPTED or DECLINED, and when. */
    public void answer(RequestStatus answer) {
        this.status = answer;
        this.respondedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getLearnerId() {
        return learnerId;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public String getMessage() {
        return message;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
