package com.tuneacademy.backend.scoring;

/**
 * Anything that compares a student's recording with the reference.
 * R is the kind of result it returns, the same way List<String> says what a list holds.
 * Each scorer has its own: timing adds the start offset, notes add a status per note.
 */
public interface Scorer<R> {

    R score(AudioAnalysis reference, AudioAnalysis student);
}
