package com.tuneacademy.backend.practice;

import com.tuneacademy.backend.scoring.AudioAnalysis;
import com.tuneacademy.backend.scoring.NoteScorer;
import com.tuneacademy.backend.scoring.TimingScorer;

/**
 * The details of one attempt beyond the three scores, saved as JSON in attempts.analysis.
 * The result page reads it for "you came in 0.3 s late" and the colored row of notes.
 *
 * @param student what was measured from the student's recording
 * @param timing  the timing details (start offset, hits, missed, extra)
 * @param notes   the status of each note; null when the exercise isn't monophonic
 */
public record AttemptAnalysis(AudioAnalysis student, TimingScorer.Result timing, NoteScorer.Result notes) {}
