package com.tuneacademy.backend.tutoring;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Created when a request is accepted: the learner and instructor are now working together.
 * Its id IS the request's id (one engagement per accepted request), and the learner, instructor
 * and message are read from that request rather than copied here.
 * status and ended_at aren't mapped: ending engagements is cut, so status stays at its default, ACTIVE.
 */
@Entity
@Table(name = "engagements")
public class Engagement {

    @Id
    private Long requestId;

    private Instant startedAt = Instant.now();

    protected Engagement() {
    }

    public Engagement(Long requestId) {
        this.requestId = requestId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
