package com.tuneacademy.backend.scoring;

import java.util.List;

/**
 * What we measure from one recording. The scorers compare two of these: reference vs student.
 * Saved as JSON (exercises.reference_analysis, attempts.analysis) and drawn as the overlay.
 *
 * @param noteStarts when each note starts, in seconds
 * @param notes      the note number played at each start (an entry is null if no clear pitch was heard).
 *                   The whole list is null when notes weren't detected (exercise isn't monophonic).
 */
public record AudioAnalysis(List<Double> noteStarts, List<Integer> notes) {

    public static AudioAnalysis of(AudioClip clip, boolean detectNotes) {
        List<Double> starts = NoteStartDetector.find(clip);
        List<Integer> notes = detectNotes ? NoteDetector.find(clip, starts) : null;
        return new AudioAnalysis(starts, notes);
    }
}
