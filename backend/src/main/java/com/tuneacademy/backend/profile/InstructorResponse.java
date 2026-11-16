package com.tuneacademy.backend.profile;

/**
 * An instructor as anyone browsing can see them. There's deliberately no email field: contact
 * details are only shared once a tutoring request is accepted.
 */
public record InstructorResponse(
    Long id,
    String fullName,
    String bio,
    int experienceYears,
    String nationality,
    String[] specialties,
    String[] teachingLevels,
    int hourlyRateCents
) {}
