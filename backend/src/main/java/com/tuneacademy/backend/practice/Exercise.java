package com.tuneacademy.backend.practice;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tuneacademy.backend.scoring.AudioAnalysis;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A piece to practice. Rows are created by the seed migration, not by Java code. */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String instrument;
    private boolean monophonic = true; // one note at a time; false = chords (not scored yet)
    private String audioPath;          // reference recording inside the app's resources; null = no audio yet
    private String attribution;

    // A JSONB column. Hibernate converts the record to JSON on save and back on load.
    // Null until the startup analyzer has analyzed the reference recording.
    @JdbcTypeCode(SqlTypes.JSON)
    private AudioAnalysis referenceAnalysis;

    private Instant createdAt = Instant.now();

    protected Exercise() {} // for JPA

    /** Can students practice it? Only once the reference has been analyzed. */
    public boolean isAvailable() {
        return referenceAnalysis != null;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getInstrument() { return instrument; }
    public boolean isMonophonic() { return monophonic; }
    public String getAudioPath() { return audioPath; }
    public String getAttribution() { return attribution; }
    public AudioAnalysis getReferenceAnalysis() { return referenceAnalysis; }
    public Instant getCreatedAt() { return createdAt; }

    public void setReferenceAnalysis(AudioAnalysis referenceAnalysis) { this.referenceAnalysis = referenceAnalysis; }
}
