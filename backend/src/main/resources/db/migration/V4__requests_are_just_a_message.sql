-- V4: a tutoring request is now just a message. Time slots and lesson counts are cut,
-- so `weeks` no longer has to be filled in. (`message` already exists, and `session_type`
-- and `weekly_slots` already have defaults, so they need no change.)
ALTER TABLE tutoring_requests ALTER COLUMN weeks DROP NOT NULL;
