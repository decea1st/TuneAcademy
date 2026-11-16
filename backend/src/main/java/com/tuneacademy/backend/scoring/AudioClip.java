package com.tuneacademy.backend.scoring;

/**
 * A recording as plain numbers: the sound wave as values from -1 to 1 (0 is silence),
 * plus how many of those values there are per second (the sample rate, e.g. 44100 or 48000).
 */
public record AudioClip(float[] samples, int sampleRate) {

    public double durationSeconds() {
        return (double) samples.length / sampleRate;
    }

    /**
     * How loud the samples from index `from` up to (not including) `to` are.
     * Squaring each value makes swings below 0 count the same as swings above it;
     * then average, then square-root to get back to the original scale (called "RMS").
     */
    public double loudness(int from, int to) {
        if (to <= from) return 0;
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += samples[i] * samples[i];
        }
        return Math.sqrt(sum / (to - from));
    }
}
