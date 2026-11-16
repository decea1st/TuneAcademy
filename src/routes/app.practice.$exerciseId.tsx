import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { ChevronLeft, Loader2, Mic, Square } from "lucide-react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Card } from "@/components/tuneacademy/Card";
import { Pill } from "@/components/tuneacademy/Pill";
import { useAuth } from "@/contexts/AuthContext";
import { useRecorder } from "@/hooks/useRecorder";
import {
  getExercises,
  getMyAttempts,
  referenceAudioUrl,
  submitAttempt,
  type AttemptSummary,
  type Exercise,
} from "@/lib/practice";

export const Route = createFileRoute("/app/practice/$exerciseId")({
  head: () => ({ meta: [{ title: "Practice - TuneAcademy" }] }),
  component: ExercisePage,
});

/** Same limit as the backend's PracticeService.MAX_SECONDS. */
const MAX_SECONDS = 30;

const card = "border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur";
const heading = "text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]";
const muted = "text-sm font-semibold text-[#e8f4df]/62";

function ExercisePage() {
  const id = Number(Route.useParams().exerciseId);
  const { isGuest } = useAuth();
  const navigate = useNavigate();
  const recorder = useRecorder(MAX_SECONDS);

  const [exercise, setExercise] = useState<Exercise | null>(null);
  const [pastScores, setPastScores] = useState<AttemptSummary[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (isGuest) return; // the endpoints need a login
    // There's no single-exercise endpoint; the list is tiny, so we pick ours out of it.
    Promise.all([getExercises(), getMyAttempts(id)])
      .then(([all, mine]) => {
        const found = all.find((e) => e.id === id);
        if (!found) throw new Error("Exercise not found.");
        setExercise(found);
        setPastScores(mine);
      })
      .catch((e: Error) => setLoadError(e.message));
  }, [id, isGuest]);

  async function submit() {
    if (!recorder.wav) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const attempt = await submitAttempt(id, recorder.wav);
      void navigate({
        to: "/app/practice/attempts/$attemptId",
        params: { attemptId: String(attempt.id) },
      });
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "Upload failed.");
      setSubmitting(false);
    }
  }

  let content;
  if (isGuest) {
    content = (
      <p className={muted}>
        <Link to="/login" className="text-[#ffd666] underline">
          Log in
        </Link>{" "}
        to practice.
      </p>
    );
  } else if (loadError) {
    content = <p className="text-sm text-destructive">{loadError}</p>;
  } else if (!exercise) {
    content = <p className={muted}>Loading…</p>;
  } else {
    content = (
      <>
        <header>
          <p className={heading}>{exercise.instrument}</p>
          <h1 className="mt-2 text-4xl font-black text-[#fffdf5]">{exercise.title}</h1>
          {exercise.attribution && (
            <p className="mt-2 text-xs text-[#e8f4df]/42">{exercise.attribution}</p>
          )}
        </header>

        {!exercise.available ? (
          <p className={muted}>This exercise isn't available yet.</p>
        ) : (
          <>
            <Card className={card}>
              <h2 className={heading}>1. Listen</h2>
              {/* The browser sends the login cookie with this request too, so it works on a protected endpoint. */}
              <audio controls preload="none" src={referenceAudioUrl(id)} className="mt-3 w-full" />
            </Card>

            <Card className={card}>
              <h2 className={heading}>2. Record</h2>
              <p className={`mt-2 ${muted}`}>
                Press record, then play. Recording stops by itself at {MAX_SECONDS} seconds.
              </p>
              <div className="mt-4 flex items-center gap-4">
                <Pill
                  className="bg-[#ffd666] text-[#0b1510]"
                  disabled={submitting}
                  onClick={recorder.recording ? recorder.stop : () => void recorder.start()}
                >
                  {recorder.recording ? (
                    <Square className="h-4 w-4" />
                  ) : (
                    <Mic className="h-4 w-4" />
                  )}
                  {recorder.recording ? "Stop" : recorder.wav ? "Record again" : "Record"}
                </Pill>
                <span className="text-lg font-bold tabular-nums text-[#fffdf5]">
                  {formatTime(recorder.seconds)} / {formatTime(MAX_SECONDS)}
                </span>
                {recorder.recording && (
                  <span className="h-3 w-3 animate-pulse rounded-full bg-red-500" />
                )}
              </div>
              {recorder.error && <p className="mt-3 text-sm text-destructive">{recorder.error}</p>}

              {recorder.wav && !recorder.recording && (
                <Pill className="mt-4" disabled={submitting} onClick={() => void submit()}>
                  {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  {submitting ? "Scoring…" : "Submit for scoring"}
                </Pill>
              )}
              {submitError && <p className="mt-3 text-sm text-destructive">{submitError}</p>}
            </Card>
          </>
        )}

        <Card className={card}>
          <h2 className={heading}>Your past scores</h2>
          {pastScores.length === 0 ? (
            <p className={`mt-2 ${muted}`}>No attempts yet.</p>
          ) : (
            <ul className="mt-3 divide-y divide-[#fffdf5]/10">
              {pastScores.map((a) => (
                <li key={a.id}>
                  <Link
                    to="/app/practice/attempts/$attemptId"
                    params={{ attemptId: String(a.id) }}
                    className="flex items-center justify-between py-3 hover:text-[#ffd666]"
                  >
                    <span className={muted}>{new Date(a.createdAt).toLocaleString()}</span>
                    <span className="text-sm font-bold text-[#fffdf5]">
                      {a.totalScore}{" "}
                      <span className="font-semibold text-[#e8f4df]/62">
                        (timing {a.timingScore} · notes {a.noteScore ?? "—"})
                      </span>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </>
    );
  }

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-3xl space-y-6 px-5 pt-8">
        <Link
          to="/app/practice"
          className="inline-flex items-center gap-1 text-sm font-semibold text-[#e8f4df]/62 hover:text-[#fffdf5]"
        >
          <ChevronLeft className="h-4 w-4" /> All exercises
        </Link>
        {content}
      </div>
    </AppShell>
  );
}

function formatTime(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
