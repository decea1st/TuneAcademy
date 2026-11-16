package com.tuneacademy.backend.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 3: finds when each note starts, from sudden jumps in loudness.
 * Suits struck or plucked notes (piano, guitar), which start with a burst of sound.
 */
public final class NoteStartDetector {

    static final double SLICE_SECONDS = 0.01;  // loudness is measured every 10 ms
    static final double SILENCE = 0.01;        // quieter than this is silence or room hiss, never a note
    static final double JUMP = 2.0;            // a note starts when a slice is twice as loud as...
    static final int RECENT_SLICES = 5;        // ...the average of the 50 ms before it
    static final double MIN_GAP_SECONDS = 0.1; // two starts closer than this are the same note

    private NoteStartDetector() {}

    /** The start time of each note, in seconds from the beginning of the clip. */
    public static List<Double> find(AudioClip clip) {
        int sliceSize = (int) Math.round(clip.sampleRate() * SLICE_SECONDS); // 441 samples at 44100 Hz, 480 at 48000
        int sliceCount = clip.samples().length / sliceSize;

        double[] loudness = new double[sliceCount];
        for (int i = 0; i < sliceCount; i++) {
            loudness[i] = clip.loudness(i * sliceSize, (i + 1) * sliceSize);
        }

        List<Double> starts = new ArrayList<>();
        double lastStart = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < sliceCount; i++) {
            double time = (double) i * sliceSize / clip.sampleRate();
            boolean audible = loudness[i] > SILENCE;
            // Compared with the recent loudness, not a fixed number, so quiet and loud recordings behave the same.
            boolean jumped = loudness[i] > JUMP * averageBefore(loudness, i);
            if (audible && jumped && time - lastStart >= MIN_GAP_SECONDS) {
                starts.add(time);
                lastStart = time;
            }
        }
        return starts;
    }

    // Average loudness of the few slices just before slice i (0 at the very start of the clip).
    private static double averageBefore(double[] loudness, int i) {
        int from = Math.max(0, i - RECENT_SLICES);
        if (from == i) return 0;
        double sum = 0;
        for (int j = from; j < i; j++) {
            sum += loudness[j];
        }
        return sum / (i - from);
    }
}
