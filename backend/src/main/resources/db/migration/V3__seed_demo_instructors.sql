-- V3: three demo instructors, so the instructor list isn't empty on a fresh install
-- (including production, for the live demo). They're already onboarded.
-- All three log in with the password "demo-instructor"; only its BCrypt hash is stored.
-- example.com is reserved for examples, so these can never be someone's real address.

INSERT INTO users (email, password_hash, role, full_name, bio, onboarding_complete) VALUES
    ('maya@example.com', '$2a$10$3GrRqobqFLBMqewpap69KOzZLzX.aIxNdzACqgfrO1Y4y9tTCKKf6', 'INSTRUCTOR', 'Maya Chen',
     'Classical training, pop ears. I help beginners read music and play the songs they love.', TRUE),
    ('diego@example.com', '$2a$10$3GrRqobqFLBMqewpap69KOzZLzX.aIxNdzACqgfrO1Y4y9tTCKKf6', 'INSTRUCTOR', 'Diego Alvarez',
     'Twelve years on stage and in the studio. Rhythm first, then everything else falls into place.', TRUE),
    ('amara@example.com', '$2a$10$3GrRqobqFLBMqewpap69KOzZLzX.aIxNdzACqgfrO1Y4y9tTCKKf6', 'INSTRUCTOR', 'Amara Okafor',
     'Voice and piano. Breathing, pitch and confidence, one song at a time.', TRUE);

INSERT INTO instructor_profiles (user_id, experience_years, nationality, specialties, teaching_levels, hourly_rate_cents) VALUES
    ((SELECT id FROM users WHERE email = 'maya@example.com'), 8, 'Canada',
     '{Piano}', '{BEGINNER,INTERMEDIATE}', 4500),
    ((SELECT id FROM users WHERE email = 'diego@example.com'), 12, 'Mexico',
     '{Guitar,Bass}', '{BEGINNER,INTERMEDIATE,ADVANCED}', 4000),
    ((SELECT id FROM users WHERE email = 'amara@example.com'), 6, 'Nigeria',
     '{Voice,Piano}', '{BEGINNER,INTERMEDIATE}', 3500);
