package com.tuneacademy.backend.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether the student played the right notes. Only for monophonic exercises (one note at a time).
 * Score = reference notes played with the right pitch / all reference notes.
 * Exactly one or more octaves off counts as right, but is flagged.
 */
public class NoteScorer implements Scorer<NoteScorer.Result> {

    public enum Status { CORRECT, OCTAVE_OFF, WRONG, MISSED }

    /** One reference note: what it should be, what the student played (null if missed or unclear), and the verdict. */
    public record NoteResult(int expected, Integer played, Status status) {}

    public record Result(int score, List<NoteResult> notes) {}

    @Override
    public Result score(AudioAnalysis reference, AudioAnalysis student) {
        if (reference.notes() == null || student.notes() == null) {
            throw new IllegalArgumentException("Notes weren't detected; only monophonic exercises get a note score.");
        }
        // Same pairing as timing: each reference note is compared with the student note played at that moment.
        NotePairing pairing = NotePairing.of(reference.noteStarts(), student.noteStarts());

        List<NoteResult> results = new ArrayList<>();
        int right = 0;
        for (int r = 0; r < reference.notes().size(); r++) {
            Integer expected = reference.notes().get(r);
            if (expected == null) continue; // the reference has no clear pitch here (seeding rejects these)
            int s = pairing.studentNoteFor()[r];
            Integer played = s >= 0 ? student.notes().get(s) : null;
            Status status = judge(expected, played, s >= 0);
            if (status == Status.CORRECT || status == Status.OCTAVE_OFF) right++;
            results.add(new NoteResult(expected, played, status));
        }
        int score = results.isEmpty() ? 0 : (int) Math.round(100.0 * right / results.size());
        return new Result(score, results);
    }

    private static Status judge(int expected, Integer played, boolean paired) {
        if (!paired) return Status.MISSED;
        if (played == null) return Status.WRONG; // something was played at the right time, but with no clear pitch
        if (played == expected) return Status.CORRECT;
        // 12 note numbers = one octave: the same note, just higher or lower.
        if ((played - expected) % 12 == 0) return Status.OCTAVE_OFF;
        return Status.WRONG;
    }
}
