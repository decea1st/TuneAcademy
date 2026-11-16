package com.tuneacademy.backend.practice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.tuneacademy.backend.scoring.AudioAnalysis;
import com.tuneacademy.backend.scoring.WavReader;

/**
 * Runs once when the app starts: analyzes each exercise's reference recording and saves the result,
 * so each student attempt only has to analyze the student's audio.
 * Skips exercises with no audio yet, and ones already analyzed on an earlier start.
 */
@Component
public class ReferenceAnalyzer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceAnalyzer.class);

    private final ExerciseRepository exercises;

    public ReferenceAnalyzer(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Exercise exercise : exercises.findAll()) {
            if (exercise.getAudioPath() == null || exercise.isAvailable()) continue;
            analyze(exercise);
        }
    }

    // A bad reference is logged and left unavailable; it never stops the app from starting.
    private void analyze(Exercise exercise) {
        AudioAnalysis analysis;
        try (InputStream audio = new ClassPathResource(exercise.getAudioPath()).getInputStream()) {
            analysis = AudioAnalysis.of(WavReader.read(audio), exercise.isMonophonic());
        } catch (IOException | IllegalArgumentException e) {
            log.error("Exercise {} \"{}\": can't read reference audio {}: {}",
                exercise.getId(), exercise.getTitle(), exercise.getAudioPath(), e.getMessage());
            return;
        }

        if (analysis.noteStarts().isEmpty()) {
            log.error("Exercise {} \"{}\" rejected: no notes found in the reference.", exercise.getId(), exercise.getTitle());
            return;
        }
        if (exercise.isMonophonic() && analysis.notes().contains(null)) {
            log.error("Exercise {} \"{}\" rejected: {} of {} notes in the reference have no clear pitch.",
                exercise.getId(), exercise.getTitle(),
                Collections.frequency(analysis.notes(), null), analysis.notes().size());
            return;
        }

        exercise.setReferenceAnalysis(analysis);
        exercises.save(exercise);
        log.info("Exercise {} \"{}\": reference analyzed, {} notes.",
            exercise.getId(), exercise.getTitle(), analysis.noteStarts().size());
    }
}
