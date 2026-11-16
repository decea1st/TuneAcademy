package com.tuneacademy.backend.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.tuneacademy.backend.scoring.AudioAnalysis;
import com.tuneacademy.backend.scoring.WavReader;

import tools.jackson.databind.json.JsonMapper;

/**
 * Known-answer test on the real reference: the WAV was rendered from a MIDI file,
 * so the MIDI's exact note numbers and start times are the right answer.
 */
class ReferenceMelodyTest {

    private static final double START_TOLERANCE_SECONDS = 0.020;

    @Test
    void youSayRunMatchesTheOriginalMidi() throws IOException {
        AudioAnalysis expected;
        try (InputStream json = getClass().getResourceAsStream("/exercises/you-say-run-melody.notes.json")) {
            expected = JsonMapper.builder().build().readValue(json, AudioAnalysis.class);
        }
        AudioAnalysis actual;
        try (InputStream wav = getClass().getResourceAsStream("/exercises/you-say-run-melody.wav")) {
            actual = AudioAnalysis.of(WavReader.read(wav), true);
        }

        assertEquals(expected.notes(), actual.notes(), "note numbers");
        for (int i = 0; i < expected.noteStarts().size(); i++) {
            assertEquals(expected.noteStarts().get(i), actual.noteStarts().get(i), START_TOLERANCE_SECONDS,
                "start of note " + i);
        }
    }
}
