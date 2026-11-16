-- An exercise can be listed before its reference audio exists (shown as "coming soon").
-- audio_path is a path inside the app's resources folder (src/main/resources).
ALTER TABLE exercises ALTER COLUMN audio_path DROP NOT NULL;

-- reference_analysis starts empty; ReferenceAnalyzer fills it in at startup.
INSERT INTO exercises (title, instrument, monophonic, audio_path, attribution) VALUES
    ('You Say Run (melody)', 'piano', TRUE, 'exercises/you-say-run-melody.wav',
     'Melody from ''You Say Run'' (Yuki Hayashi), taken from a fan MIDI arrangement.'),
    ('Chord progression (C-G-Am-F)', 'guitar', FALSE, NULL, NULL);
