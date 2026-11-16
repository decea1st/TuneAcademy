package com.tuneacademy.backend.profile;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// The id is the instructor's user id, so findById(userId) finds an instructor's profile.
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, Long> {

    // The two queries below build InstructorResponse straight from the columns they name, so
    // users.email is never even read for the public instructor pages. Joining on the profile also
    // means only onboarded instructors appear: the profile row is created by onboarding.
    String PUBLIC_INSTRUCTOR = """
        select new com.tuneacademy.backend.profile.InstructorResponse(
            u.id, u.fullName, u.bio, p.experienceYears, p.nationality,
            p.specialties, p.teachingLevels, p.hourlyRateCents)
        from InstructorProfile p join User u on u.id = p.userId
        """;

    @Query(PUBLIC_INSTRUCTOR + " order by u.fullName")
    List<InstructorResponse> findAllPublic();

    @Query(PUBLIC_INSTRUCTOR + " where p.userId = :id")
    Optional<InstructorResponse> findPublicById(Long id);
}
