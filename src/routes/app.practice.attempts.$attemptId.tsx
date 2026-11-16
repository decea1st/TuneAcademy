import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { ChevronLeft } from "lucide-react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Card } from "@/components/tuneacademy/Card";
import { ScoreBar } from "@/components/tuneacademy/ScoreBar";
import { useAuth } from "@/contexts/AuthContext";
import { getAttempt, type Attempt, type NoteStatus } from "@/lib/practice";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/practice/attempts/$attemptId")({
  head: () => ({ meta: [{ title: "Result - TuneAcademy" }] }),
  component: ResultPage,
});

const card = "border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur";
const heading = "text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]";
const muted = "text-sm font-semibold text-[#e8f4df]/62";

/** How each note status looks, and what the legend calls it. */
const statusStyle: Record<NoteStatus, { label: string; className: string }> = {
  CORRECT: {
    label: "Correct",
    className: "border-emerald-400/60 bg-emerald-500/20 text-emerald-100",
  },
  OCTAVE_OFF: {
    label: "Octave off",
    className: "border-amber-400/60 bg-amber-500/20 text-amber-100",
  },
  WRONG: { label: "Wrong note", className: "border-red-400/60 bg-red-500/20 text-red-100" },
  MISSED: { label: "Missed", className: "border-dashed border-[#fffdf5]/30 text-[#fffdf5]/40" },
};

function ResultPage() {
  const id = Number(Route.useParams().attemptId);
  const { isGuest } = useAuth();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isGuest) return; // the endpoint needs a login
    getAttempt(id)
      .then(setAttempt)
      .catch((e: Error) => setError(e.message)); // someone else's attempt: "Attempt not found."
  }, [id, isGuest]);

  let content;
  if (isGuest) {
    content = (
      <p className={muted}>
        <Link to="/login" className="text-[#ffd666] underline">
          Log in
        </Link>{" "}
        to see your results.
      </p>
    );
  } else if (error) {
    content = <p className="text-sm text-destructive">{error}</p>;
  } else if (!attempt) {
    content = <p className={muted}>Loading…</p>;
  } else {
    const { timing, notes } = attempt.analysis;
    content = (
      <>
        <header>
          <p className={heading}>Your result</p>
          <p className="mt-2 text-7xl font-black tabular-nums text-[#ffd666]">
            {attempt.totalScore}
          </p>
          <p className={muted}>{new Date(attempt.createdAt).toLocaleString()}</p>
        </header>

        <Card className={`${card} space-y-4`}>
          <ScoreBar label="Timing" value={attempt.timingScore} animate />
          {attempt.noteScore !== null && (
            <ScoreBar label="Notes" value={attempt.noteScore} animate />
          )}
          <p className="text-xs text-[#e8f4df]/42">The total is the average of the two.</p>
        </Card>

        <Card className={card}>
          <h2 className={heading}>Timing</h2>
          <p className="mt-2 text-lg font-bold text-[#fffdf5]">
            {startMessage(timing.startOffsetSeconds)}
          </p>
          <p className={`mt-1 ${muted}`}>
            {timing.hits} of {timing.hits + timing.missed} notes on time
            {timing.extra > 0 && ` · ${timing.extra} extra`}. The rest of your notes are timed from
            your first one, so a late start on its own doesn't cost points.
          </p>
        </Card>

        {notes && (
          <Card className={card}>
            <h2 className={heading}>Notes</h2>
            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1">
              {(Object.keys(statusStyle) as NoteStatus[]).map((status) => (
                <span key={status} className="flex items-center gap-1.5 text-xs text-[#e8f4df]/62">
                  <span className={cn("h-3 w-3 rounded border", statusStyle[status].className)} />
                  {statusStyle[status].label} (
                  {notes.notes.filter((n) => n.status === status).length})
                </span>
              ))}
            </div>
            {/* One chip per note of the piece, in order. Hover a chip to see what you played. */}
            <div className="mt-4 flex flex-wrap gap-1.5">
              {notes.notes.map((n, i) => (
                <span
                  key={i}
                  title={
                    n.status === "MISSED"
                      ? `Expected ${noteName(n.expected)}, not played`
                      : `Expected ${noteName(n.expected)}, played ${n.played === null ? "an unclear pitch" : noteName(n.played)}`
                  }
                  className={cn(
                    "min-w-10 rounded-md border px-1.5 py-1 text-center text-xs font-bold",
                    statusStyle[n.status].className,
                  )}
                >
                  {noteName(n.expected)}
                </span>
              ))}
            </div>
          </Card>
        )}
      </>
    );
  }

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-3xl space-y-6 px-5 pt-8">
        {attempt ? (
          <Link
            to="/app/practice/$exerciseId"
            params={{ exerciseId: String(attempt.exerciseId) }}
            className="inline-flex items-center gap-1 text-sm font-semibold text-[#e8f4df]/62 hover:text-[#fffdf5]"
          >
            <ChevronLeft className="h-4 w-4" /> Back to the exercise
          </Link>
        ) : (
          <Link
            to="/app/practice"
            className="inline-flex items-center gap-1 text-sm font-semibold text-[#e8f4df]/62 hover:text-[#fffdf5]"
          >
            <ChevronLeft className="h-4 w-4" /> All exercises
          </Link>
        )}
        {content}
      </div>
    </AppShell>
  );
}

/** The offset is student minus reference, so positive means late. Within 50 ms counts as on time. */
function startMessage(offsetSeconds: number | null) {
  if (offsetSeconds === null) return "We couldn't hear any notes in this recording.";
  if (Math.abs(offsetSeconds) < 0.05) return "You came in right on time.";
  return `You came in ${Math.abs(offsetSeconds).toFixed(2)} s ${offsetSeconds > 0 ? "late" : "early"}.`;
}

const NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];

/** Note number to name, the same numbering as the backend: 60 = C4 (middle C), 69 = A4 (440 Hz). */
function noteName(noteNumber: number) {
  return NOTE_NAMES[noteNumber % 12] + (Math.floor(noteNumber / 12) - 1);
}
