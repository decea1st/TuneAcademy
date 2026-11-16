package com.tuneacademy.backend.scoring;

/**
 * Step 4: how well the student's notes line up in time with the reference.
 * Score = notes hit / (reference notes + extra notes), so missed and extra notes both lower it.
 */
public class TimingScorer implements Scorer<TimingScorer.Result> {

    /** startOffsetSeconds: 0.3 means "you came in 0.3 s late"; null if no notes were heard. */
    public record Result(int score, Double startOffsetSeconds, int hits, int missed, int extra) {}

    @Override
    public Result score(AudioAnalysis reference, AudioAnalysis student) {
        NotePairing pairing = NotePairing.of(reference.noteStarts(), student.noteStarts());
        int hits = pairing.hits();
        int missed = reference.noteStarts().size() - hits;
        int outOf = reference.noteStarts().size() + pairing.extraNotes();
        int score = outOf == 0 ? 0 : (int) Math.round(100.0 * hits / outOf);
        return new Result(score, pairing.startOffsetSeconds(), hits, missed, pairing.extraNotes());
    }
}
