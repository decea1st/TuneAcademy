-- V1: initial schema. Flyway runs this once, on the first Spring Boot startup against an empty database.
-- Never edit this file after it has run; put changes in V2__something.sql instead.

-- Everyone who can log in, learner or instructor.
CREATE TABLE users (
    id                  BIGSERIAL   PRIMARY KEY,
    email               TEXT        NOT NULL UNIQUE,
    password_hash       TEXT        NOT NULL,
    role                TEXT        NOT NULL CHECK (role IN ('LEARNER', 'INSTRUCTOR')),
    full_name           TEXT        NOT NULL,
    bio                 TEXT        NOT NULL DEFAULT '',
    skill_level         TEXT        CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    instruments         TEXT[]      NOT NULL DEFAULT '{}',
    onboarding_complete BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Extra fields only instructors have. One row per instructor (one-to-one with users).
-- No rating or review_count columns: those are calculated from the reviews table when needed.
CREATE TABLE instructor_profiles (
    user_id             BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    experience_years    INTEGER     NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
    nationality         TEXT,
    specialties         TEXT[]      NOT NULL DEFAULT '{}',
    teaching_levels     TEXT[]      NOT NULL DEFAULT '{}',
    hourly_rate_cents   INTEGER     NOT NULL CHECK (hourly_rate_cents >= 0),
    weekly_availability JSONB       NOT NULL DEFAULT '[]',
    max_tutoring_weeks  INTEGER     CHECK (max_tutoring_weeks > 0)
);

-- ---------------------------------------------------------------------------
-- Marketplace
-- ---------------------------------------------------------------------------

-- A learner's rating of an instructor. One review per learner per instructor.
CREATE TABLE reviews (
    id                  BIGSERIAL   PRIMARY KEY,
    instructor_id       BIGINT      NOT NULL REFERENCES instructor_profiles (user_id) ON DELETE CASCADE,
    learner_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    stars               INTEGER     NOT NULL CHECK (stars BETWEEN 1 AND 5),
    review_text         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (instructor_id, learner_id)
);

-- A learner asking an instructor for lessons. The instructor accepts or declines it.
CREATE TABLE tutoring_requests (
    id                  BIGSERIAL   PRIMARY KEY,
    learner_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    instructor_id       BIGINT      NOT NULL REFERENCES instructor_profiles (user_id) ON DELETE CASCADE,
    session_type        TEXT        NOT NULL DEFAULT 'SOLO' CHECK (session_type IN ('SOLO', 'GROUP')),
    weekly_slots        JSONB       NOT NULL DEFAULT '[]',
    weeks               INTEGER     NOT NULL CHECK (weeks > 0),
    message             TEXT        NOT NULL DEFAULT '',
    status              TEXT        NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at        TIMESTAMPTZ
);

-- A learner can't send the same instructor a second request while the first is still pending.
CREATE UNIQUE INDEX one_pending_request_per_pair
    ON tutoring_requests (learner_id, instructor_id) WHERE status = 'PENDING';
-- Speeds up "show me my incoming requests" for the instructor.
CREATE INDEX ON tutoring_requests (instructor_id, status);

-- Created when a request is accepted. One per accepted request (one-to-one, like instructor_profiles).
-- The learner, instructor, slots and weeks are read from the request, so they aren't copied here.
CREATE TABLE engagements (
    request_id          BIGINT      PRIMARY KEY REFERENCES tutoring_requests (id) ON DELETE CASCADE,
    status              TEXT        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED')),
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at            TIMESTAMPTZ
);

-- Direct messages between two users. A conversation is every message between the same pair.
CREATE TABLE messages (
    id                  BIGSERIAL   PRIMARY KEY,
    sender_id           BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body                TEXT        NOT NULL CHECK (body <> ''),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at             TIMESTAMPTZ,
    CHECK (sender_id <> recipient_id)
);

-- Polling asks "any messages in this conversation newer than id X?"; this makes that fast.
CREATE INDEX ON messages (sender_id, recipient_id, id);
-- Speeds up unread counts for the recipient.
CREATE INDEX ON messages (recipient_id, read_at);

-- ---------------------------------------------------------------------------
-- Practice module (shares only the users table with the marketplace)
-- ---------------------------------------------------------------------------

-- A seeded exercise with a reference recording to play along with.
CREATE TABLE exercises (
    id                  BIGSERIAL   PRIMARY KEY,
    title               TEXT        NOT NULL,
    instrument          TEXT        NOT NULL,
    monophonic          BOOLEAN     NOT NULL DEFAULT TRUE,  -- one note at a time; YIN can't score chords, so FALSE means timing-only
    audio_path          TEXT        NOT NULL,  -- reference recording, relative to the storage folder
    attribution         TEXT,                  -- credit line for Creative Commons audio; empty for your own recordings
    reference_analysis  JSONB,                 -- pitch track + loudness envelope, computed once when seeded
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One scored try at an exercise. The audio file is deleted after scoring; only these results are kept.
CREATE TABLE attempts (
    id                  BIGSERIAL   PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id         BIGINT      NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    timing_score        INTEGER     NOT NULL CHECK (timing_score BETWEEN 0 AND 100),
    note_score          INTEGER     CHECK (note_score BETWEEN 0 AND 100),  -- empty when the exercise isn't monophonic
    total_score         INTEGER     NOT NULL CHECK (total_score BETWEEN 0 AND 100),
    analysis            JSONB       NOT NULL,  -- the student's pitch track + envelope, drawn on the overlay
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Speeds up the progress page: one user's attempts at one exercise, in order.
CREATE INDEX ON attempts (user_id, exercise_id, created_at);