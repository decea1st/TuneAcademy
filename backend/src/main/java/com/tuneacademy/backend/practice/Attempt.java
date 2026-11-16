package com.tuneacademy.backend.practice;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One scored try at an exercise. Only the results are kept; the audio never is. Never changes once saved. */
@Entity
@Table(name = "attempts")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain IDs rather than linked objects: all we ever need is "whose is it" and "which exercise".
    // The database's foreign keys still guarantee both point at real rows.
    private Long userId;
    private Long exerciseId;

    private int timingScore;
    private Integer noteScore; // null when the exercise isn't monophonic
    private int totalScore;

    @JdbcTypeCode(SqlTypes.JSON)
    private AttemptAnalysis analysis;

    private Instant createdAt = Instant.now();

    protected Attempt() {} // for JPA

    public Attempt(Long userId, Long exerciseId, int timingScore, Integer noteScore, int totalScore, AttemptAnalysis analysis) {
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.timingScore = timingScore;
        this.noteScore = noteScore;
        this.totalScore = totalScore;
        this.analysis = analysis;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public int getTimingScore() { return timingScore; }
    public Integer getNoteScore() { return noteScore; }
    public int getTotalScore() { return totalScore; }
    public AttemptAnalysis getAnalysis() { return analysis; }
    public Instant getCreatedAt() { return createdAt; }
}
