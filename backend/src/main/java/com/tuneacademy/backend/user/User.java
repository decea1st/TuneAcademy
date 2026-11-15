package com.tuneacademy.backend.user;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB generated an id, not Java.
    private Long id;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING) // Without this, the enum's index is saved (0 or 1)
    private Role role;

    private String fullName;

    // Fields with a DEFAULT in the SQL start with the same value here,
    // because Hibernate always sends every field when it saves a row.
    private String bio = "";

    @Enumerated(EnumType.STRING)
    private SkillLevel skillLevel;

    private String[] instruments = new String[0];

    private boolean onboardingComplete = false;

    private Instant createdAt = Instant.now();

    // JPA uses this to create an empty User before filling it in from a database row.
    protected User() {
    }

    // What your code uses at signup: the fields every new user must have.
    public User(String email, String passwordHash, Role role, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
    }

    // No setters for id, role or createdAt: they're set once and never change.

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String[] getInstruments() {
        return instruments;
    }

    public void setInstruments(String[] instruments) {
        this.instruments = instruments;
    }

    public boolean isOnboardingComplete() {
        return onboardingComplete;
    }

    public void setOnboardingComplete(boolean onboardingComplete) {
        this.onboardingComplete = onboardingComplete;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
