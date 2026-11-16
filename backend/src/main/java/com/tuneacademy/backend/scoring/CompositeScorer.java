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
        // Notes are only detected for monophonic exercises; without them there's nothing to compare.
        boolean hasNotes = reference.notes() != null && student.notes() != null;
        // Drop the student's sounds with no pitch (clicks, coughs) before anything else. Otherwise a click
        // before the music would be taken as the first note, and every other note lined up from it.
        // (The reference never has any: the startup check rejects a reference with an unpitched note.)
        if (hasNotes) student = student.pitchedOnly();

        TimingScorer.Result timing = timingScorer.score(reference, student);
        NoteScorer.Result notes = hasNotes ? noteScorer.score(reference, student) : null;
        int total = notes == null ? timing.score() : (int) Math.round((timing.score() + notes.score()) / 2.0);
        return new Result(total, timing, notes);
    }
}
