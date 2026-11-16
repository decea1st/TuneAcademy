package com.tuneacademy.backend.scoring;

import java.util.ArrayList;
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

    /** A copy with only the sounds that had a pitch. A click or a cough has none, so it isn't a note. */
    public AudioAnalysis pitchedOnly() {
        if (notes == null) return this; // notes weren't detected, so we can't tell clicks from notes
        List<Double> keptStarts = new ArrayList<>();
        List<Integer> keptNotes = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i) != null) {
                keptStarts.add(noteStarts.get(i));
                keptNotes.add(notes.get(i));
            }
        }
        return new AudioAnalysis(keptStarts, keptNotes);
    }
}
