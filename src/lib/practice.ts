import { apiFetch } from "@/lib/api";

/*
 * The practice endpoints, and the JSON shapes they return.
 * These mirror the records in PracticeController.java.
 */

export type Exercise = {
  id: number;
  title: string;
  instrument: string;
  monophonic: boolean;
  attribution: string | null;
  /** False until the backend has analyzed a reference recording (chord exercises, for now). */
  available: boolean;
};

export type NoteStatus = "CORRECT" | "OCTAVE_OFF" | "WRONG" | "MISSED";

export type Attempt = {
  id: number;
  exerciseId: number;
  totalScore: number;
  timingScore: number;
  noteScore: number | null;
  analysis: {
    timing: {
      score: number;
      /** Positive = the student came in late, negative = early. */
      startOffsetSeconds: number | null;
      hits: number;
      missed: number;
      extra: number;
    };
    notes: {
      score: number;
      notes: { expected: number; played: number | null; status: NoteStatus }[];
    } | null;
  };
  createdAt: string;
};

export type AttemptSummary = Pick<
  Attempt,
  "id" | "totalScore" | "timingScore" | "noteScore" | "createdAt"
>;

export const getExercises = () => apiFetch<Exercise[]>("/api/exercises");

export const referenceAudioUrl = (exerciseId: number) => `/api/exercises/${exerciseId}/audio`;

/** Uploads the recording as a form with one file field named "audio"; the backend scores it right away. */
export function submitAttempt(exerciseId: number, wav: Blob) {
  const form = new FormData();
  form.append("audio", wav, "attempt.wav");
  return apiFetch<Attempt>(`/api/exercises/${exerciseId}/attempts`, { method: "POST", body: form });
}

export const getAttempt = (attemptId: number) => apiFetch<Attempt>(`/api/attempts/${attemptId}`);

export const getMyAttempts = (exerciseId: number) =>
  apiFetch<AttemptSummary[]>(`/api/exercises/${exerciseId}/attempts`);
