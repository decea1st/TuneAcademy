package com.tuneacademy.backend.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The extra details only instructors have. One row per instructor: its id IS the instructor's
 * user id (one-to-one), so there's no separate id to generate.
 *
 * The table's weekly_availability and max_tutoring_weeks columns aren't mapped here: time slots
 * are cut, so those columns just keep their database defaults.
 */
@Entity
@Table(name = "instructor_profiles")
public class InstructorProfile {

    @Id
    private Long userId;

    private int experienceYears;

    private String nationality;

    private String[] specialties = new String[0];

    private String[] teachingLevels = new String[0]; // SkillLevel names, e.g. "BEGINNER"

    // Money as whole cents, never a decimal: 0.1 + 0.2 isn't exactly 0.3 in floating point.
    private int hourlyRateCents;

    protected InstructorProfile() {
    }

    public InstructorProfile(Long userId) {
        this.userId = userId;
    }

    public void update(int experienceYears, String nationality, String[] specialties, String[] teachingLevels,
                       int hourlyRateCents) {
        this.experienceYears = experienceYears;
        this.nationality = nationality;
        this.specialties = specialties;
        this.teachingLevels = teachingLevels;
        this.hourlyRateCents = hourlyRateCents;
    }

    public Long getUserId() {
        return userId;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public String getNationality() {
        return nationality;
    }

    public String[] getSpecialties() {
        return specialties;
    }

    public String[] getTeachingLevels() {
        return teachingLevels;
    }

    public int getHourlyRateCents() {
        return hourlyRateCents;
    }
}
