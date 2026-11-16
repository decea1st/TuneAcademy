package com.tuneacademy.backend.scoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.Yin;

/**
 * Which note is played after each note start.
 * Notes are numbered like MIDI: 69 is the A at 440 Hz, 60 is middle C, +1 per piano key.
 */
public final class NoteDetector {

    // About 45 ms of audio. A 10 ms slice is shorter than one wave of a low guitar note
    // (82 Hz = 12 ms per wave), and YIN needs a few waves in view to measure the pitch.
    static final int WINDOW = 2048;

    private NoteDetector() {}

    /** One note number per note start, or null where no clear pitch was heard. */
    public static List<Integer> find(AudioClip clip, List<Double> noteStarts) {
        float[] samples = clip.samples();
        int hop = (int) Math.round(clip.sampleRate() * NoteStartDetector.SLICE_SECONDS); // move 10 ms; windows overlap
        Yin yin = new Yin(clip.sampleRate(), WINDOW);

        // 1. A note number every 10 ms, timed at the middle of its window.
        List<Double> times = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for (int start = 0; start + WINDOW <= samples.length; start += hop) {
            if (clip.loudness(start, start + WINDOW) < NoteStartDetector.SILENCE) continue;
            PitchDetectionResult pitch = yin.getPitch(Arrays.copyOfRange(samples, start, start + WINDOW));
            if (!pitch.isPitched()) continue; // YIN found no steady pitch (noise, or the messy start of a note)
            times.add((start + WINDOW / 2.0) / clip.sampleRate());
            numbers.add(toNoteNumber(pitch.getPitch()));
        }

        // 2. Between two note starts, the most common note number is the note played there.
        List<Integer> notes = new ArrayList<>();
        for (int k = 0; k < noteStarts.size(); k++) {
            double from = noteStarts.get(k);
            double to = k + 1 < noteStarts.size() ? noteStarts.get(k + 1) : Double.POSITIVE_INFINITY;
            Map<Integer, Integer> counts = new HashMap<>();
            for (int i = 0; i < times.size(); i++) {
                if (times.get(i) >= from && times.get(i) < to) {
                    counts.merge(numbers.get(i), 1, Integer::sum); // add 1 to that note's count
                }
            }
            notes.add(mostCommon(counts));
        }
        return notes;
    }

    // converts a frequency to a note number with a formula
    // 440 Hz is note 69. Each note up multiplies the frequency by the 12th root of 2, so 12 notes up
    // doubles it (one octave). Rounding to the nearest whole note forgives a slightly out-of-tune instrument.
    static int toNoteNumber(double hz) {
        return (int) Math.round(12 * Math.log(hz / 440.0) / Math.log(2) + 69);
    }

    private static Integer mostCommon(Map<Integer, Integer> counts) {
        Integer best = null;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best; // null if no pitch was heard in this stretch
    }
}
