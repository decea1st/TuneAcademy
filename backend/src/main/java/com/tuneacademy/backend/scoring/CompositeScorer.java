package com.tuneacademy.backend.scoring;

/**
 * The one scorer the Practice module calls. It is itself a Scorer, built from the other two
 * (the "composite" design pattern): it runs both and combines them into the total.
 */
public class CompositeScorer implements Scorer<CompositeScorer.Result> {

    /** notes is null when the exercise isn't monophonic; then the total is the timing score alone. */
    public record Result(int totalScore, TimingScorer.Result timing, NoteScorer.Result notes) {}

    private final TimingScorer timingScorer = new TimingScorer();
    private final NoteScorer noteScorer = new NoteScorer();

    @Override
    public Result score(AudioAnalysis reference, AudioAnalysis student) {
        TimingScorer.Result timing = timingScorer.score(reference, student);
        // Notes are only detected for monophonic exercises; without them there's nothing to compare.
        boolean hasNotes = reference.notes() != null && student.notes() != null;
        NoteScorer.Result notes = hasNotes ? noteScorer.score(reference, student) : null;
        int total = notes == null ? timing.score() : (int) Math.round((timing.score() + notes.score()) / 2.0);
        return new Result(total, timing, notes);
    }
}
