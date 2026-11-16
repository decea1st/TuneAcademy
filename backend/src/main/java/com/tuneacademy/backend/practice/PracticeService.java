package com.tuneacademy.backend.practice;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.tuneacademy.backend.scoring.AudioAnalysis;
import com.tuneacademy.backend.scoring.AudioClip;
import com.tuneacademy.backend.scoring.CompositeScorer;
import com.tuneacademy.backend.scoring.WavReader;

@Service
public class PracticeService {

    static final double MAX_SECONDS = 30;

    private final ExerciseRepository exercises;
    private final AttemptRepository attempts;
    private final CompositeScorer scorer = new CompositeScorer();

    public PracticeService(ExerciseRepository exercises, AttemptRepository attempts) {
        this.exercises = exercises;
        this.attempts = attempts;
    }

    public List<Exercise> allExercises() {
        return exercises.findAll(Sort.by("id"));
    }

    public Exercise findExercise(Long id) {
        return exercises.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found."));
    }

    /**
     * Upload → read → analyze → score → save the results. The audio only ever exists in memory
     * during this method; once it returns, nothing of the recording is kept but the results.
     */
    public Attempt scoreAttempt(Long userId, Long exerciseId, MultipartFile upload) {
        Exercise exercise = findExercise(exerciseId);
        if (!exercise.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This exercise isn't available yet.");
        }

        AudioClip clip;
        try (InputStream audio = upload.getInputStream()) {
            clip = WavReader.read(audio);
        } catch (IllegalArgumentException e) { // not a WAV, or not 16-bit mono
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read the uploaded recording.");
        }
        if (clip.durationSeconds() > MAX_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recordings can be at most 30 seconds.");
        }

        AudioAnalysis student = AudioAnalysis.of(clip, exercise.isMonophonic());
        CompositeScorer.Result result = scorer.score(exercise.getReferenceAnalysis(), student);

        Integer noteScore = result.notes() == null ? null : result.notes().score();
        AttemptAnalysis details = new AttemptAnalysis(student, result.timing(), result.notes());
        return attempts.save(new Attempt(userId, exerciseId, result.timing().score(), noteScore, result.totalScore(), details));
    }

    /** Someone else's attempt gets the same 404 as one that doesn't exist, so IDs can't be probed. */
    public Attempt findMyAttempt(Long userId, Long attemptId) {
        return attempts.findByIdAndUserId(attemptId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found."));
    }

    public List<Attempt> myAttempts(Long userId, Long exerciseId) {
        findExercise(exerciseId); // 404 if the exercise doesn't exist
        return attempts.findByUserIdAndExerciseIdOrderByCreatedAtDesc(userId, exerciseId);
    }
}
