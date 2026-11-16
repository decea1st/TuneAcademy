package com.tuneacademy.backend.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Test;

/**
 * Known-answer tests: the audio is generated in code, so we know exactly which notes are in it and when.
 * Plain JUnit: no Spring and no database, so these run in seconds.
 */
class ScoringEngineTest {

    // C major scale as note numbers (60 = middle C).
    private static final int[] SCALE = {60, 62, 64, 65, 67, 69, 71, 72};

    // The "teacher's recording" every test compares against: the scale, starting at 0.5 s.
    private static final AudioAnalysis REFERENCE = analyze(plucked(44100, 0.5, 0.5, SCALE));

    private final CompositeScorer scorer = new CompositeScorer();

    @Test
    void identicalRecordingScores100() {
        CompositeScorer.Result result = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.5, SCALE)));
        assertEquals(100, result.timing().score());
        assertEquals(100, result.notes().score());
        assertEquals(100, result.totalScore());
    }

    @Test
    void startingLateStillScoresHighAndSaysHowLate() {
        // Same scale, but starting at 0.8 s instead of 0.5 s.
        CompositeScorer.Result result = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.8, SCALE)));
        assertEquals(100, result.totalScore());
        assertEquals(0.3, result.timing().startOffsetSeconds(), 0.02);
    }

    @Test
    void wrongPitchesLowerOnlyTheNoteScore() {
        int[] wrong = {62, 64, 66, 67, 69, 71, 73, 74}; // every note 2 higher: right rhythm, wrong notes
        CompositeScorer.Result result = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.5, wrong)));
        assertEquals(100, result.timing().score());
        assertEquals(0, result.notes().score());
    }

    @Test
    void silenceScoresZero() {
        AudioClip silence = new AudioClip(new float[44100 * 5], 44100);
        CompositeScorer.Result result = scorer.score(REFERENCE, analyze(silence));
        assertEquals(0, result.totalScore());
        assertNull(result.timing().startOffsetSeconds());
    }

    @Test
    void halfVolumeGivesTheSameScore() {
        int full = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.5, SCALE))).totalScore();
        int half = scorer.score(REFERENCE, analyze(plucked(44100, 0.25, 0.5, SCALE))).totalScore();
        assertEquals(full, half);
    }

    @Test
    void lowNoteIsDetected() {
        // Note 40 = 82.4 Hz, the lowest string on a guitar.
        assertEquals(List.of(40, 40, 40), analyze(plucked(44100, 0.5, 0.5, 40, 40, 40)).notes());
    }

    @Test
    void file48000HzGivesTheSameResultAs44100Hz() throws IOException {
        AudioClip at44 = WavReader.read(new ByteArrayInputStream(toWav(plucked(44100, 0.5, 0.5, SCALE))));
        AudioClip at48 = WavReader.read(new ByteArrayInputStream(toWav(plucked(48000, 0.5, 0.5, SCALE))));
        assertEquals(48000, at48.sampleRate()); // read from the file, not assumed

        AudioAnalysis analysis44 = analyze(at44);
        AudioAnalysis analysis48 = analyze(at48);
        assertEquals(analysis44.notes(), analysis48.notes());
        assertEquals(scorer.score(REFERENCE, analysis44).totalScore(), scorer.score(REFERENCE, analysis48).totalScore());
    }

    @Test
    void noteOneOctaveOffCountsButIsFlagged() {
        int[] played = SCALE.clone();
        played[7] = 84; // the last C, one octave higher
        NoteScorer.Result notes = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.5, played))).notes();
        assertEquals(100, notes.score());
        assertEquals(NoteScorer.Status.OCTAVE_OFF, notes.notes().get(7).status());
    }

    @Test
    void oneWrongNoteInAScaleIsFlaggedAtThatNote() {
        int[] played = SCALE.clone();
        played[3] = 66; // F# instead of F
        NoteScorer.Result notes = scorer.score(REFERENCE, analyze(plucked(44100, 0.5, 0.5, played))).notes();
        assertEquals(NoteScorer.Status.WRONG, notes.notes().get(3).status());
        assertEquals(66, notes.notes().get(3).played());
        for (int i = 0; i < SCALE.length; i++) {
            if (i != 3) assertEquals(NoteScorer.Status.CORRECT, notes.notes().get(i).status());
        }
        assertEquals(88, notes.score()); // 7 of 8 right
    }

    @Test
    void stereoWavIsRejected() throws IOException {
        AudioFormat stereo = new AudioFormat(44100, 16, 2, true, false);
        byte[] wav = writeWav(new byte[44100 * 4], stereo); // 1 s of stereo silence
        assertThrows(IllegalArgumentException.class, () -> WavReader.read(new ByteArrayInputStream(wav)));
    }

    // ---- Fake audio ----

    private static AudioAnalysis analyze(AudioClip clip) {
        return AudioAnalysis.of(clip, true);
    }

    /**
     * Plucked notes, like a guitar or piano: each starts suddenly, then fades.
     * One note every 0.5 s, the first at `startAt` seconds, with half a second of silence at the end.
     */
    private static AudioClip plucked(int sampleRate, double volume, double startAt, int... notes) {
        double noteSeconds = 0.5;
        float[] samples = new float[(int) ((startAt + notes.length * noteSeconds + 0.5) * sampleRate)];
        for (int k = 0; k < notes.length; k++) {
            double hz = 440 * Math.pow(2, (notes[k] - 69) / 12.0); // note number back to Hz
            int first = (int) Math.round((startAt + k * noteSeconds) * sampleRate);
            for (int i = 0; i < noteSeconds * sampleRate; i++) {
                double t = (double) i / sampleRate;
                double envelope = Math.min(1, t / 0.005) * Math.exp(-t / 0.2); // 5 ms rise, then fade out
                samples[first + i] += (float) (volume * envelope * Math.sin(2 * Math.PI * hz * t));
            }
        }
        return new AudioClip(samples, sampleRate);
    }

    /** Saves a clip as a real 16-bit mono WAV file (in memory), the same format our recording page uploads. */
    private static byte[] toWav(AudioClip clip) throws IOException {
        float[] samples = clip.samples();
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            short value = (short) Math.round(samples[i] * 32767);
            bytes[2 * i] = (byte) value;            // low byte first (little-endian)
            bytes[2 * i + 1] = (byte) (value >> 8); // then the high byte
        }
        return writeWav(bytes, new AudioFormat(clip.sampleRate(), 16, 1, true, false));
    }

    private static byte[] writeWav(byte[] bytes, AudioFormat format) throws IOException {
        long frames = bytes.length / format.getFrameSize();
        AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(bytes), format, frames);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }
}
