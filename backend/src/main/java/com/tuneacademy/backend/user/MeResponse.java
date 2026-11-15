package com.tuneacademy.backend.user;

// The JSON shape React gets for the logged-in user. It's a separate type from User
// so the password hash can never be sent to the browser by accident.
public record MeResponse(
    Long id,
    String email,
    Role role,
    String fullName,
    String bio,
    SkillLevel skillLevel,
    String[] instruments,
    boolean onboardingComplete
) {
    public static MeResponse from(User user) {
        return new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getFullName(),
            user.getBio(),
            user.getSkillLevel(),
            user.getInstruments(),
            user.isOnboardingComplete()
        );
    }
}
