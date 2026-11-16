package com.tuneacademy.backend.scoring;

import java.util.Arrays;
import java.util.List;

/**
 * Pairs each reference note with the student note played at the same moment.
 * Both scorers use this, so one missed or extra note doesn't shift every later comparison.
 *
 * @param startOffsetSeconds how far the student's first note was from the reference's
 *                           (0.3 = 0.3 s late, negative = early); null if either side has no notes
 * @param studentNoteFor     for each reference note, the index of the student note it was paired with, or -1 if missed
 * @param extraNotes         student notes that weren't paired with anything
 */
public record NotePairing(Double startOffsetSeconds, int[] studentNoteFor, int extraNotes) {

    static final double TOLERANCE_SECONDS = 0.1; // a student note within 100 ms of a reference note is a hit

    public static NotePairing of(List<Double> reference, List<Double> student) {
        int[] studentNoteFor = new int[reference.size()];
        Arrays.fill(studentNoteFor, -1);
        if (reference.isEmpty() || student.isEmpty()) {
            return new NotePairing(null, studentNoteFor, student.size());
        }

        // Line up the first notes, so starting late isn't counted against every note.
        double offset = student.get(0) - reference.get(0);

        boolean[] used = new boolean[student.size()];
        int hits = 0;
        for (int r = 0; r < reference.size(); r++) {
            // The closest unused student note within the tolerance, if any.
            int best = -1;
            double bestDistance = TOLERANCE_SECONDS;
            for (int s = 0; s < student.size(); s++) {
                double distance = Math.abs((student.get(s) - offset) - reference.get(r));
                if (!used[s] && distance <= bestDistance) {
                    best = s;
                    bestDistance = distance;
                }
            }
            if (best >= 0) {
                used[best] = true;
                studentNoteFor[r] = best;
                hits++;
            }
        }
        return new NotePairing(offset, studentNoteFor, student.size() - hits);
    }

    public int hits() {
        int hits = 0;
        for (int s : studentNoteFor) {
            if (s >= 0) hits++;
        }
        return hits;
    }
}
