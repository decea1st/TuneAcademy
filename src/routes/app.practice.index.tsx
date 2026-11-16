import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Guitar, Music2, Piano } from "lucide-react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Card } from "@/components/tuneacademy/Card";
import { useAuth } from "@/contexts/AuthContext";
import { getExercises, type Exercise } from "@/lib/practice";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/practice/")({
  head: () => ({ meta: [{ title: "Practice - TuneAcademy" }] }),
  component: PracticeList,
});

function PracticeList() {
  const { isGuest } = useAuth();
  const [exercises, setExercises] = useState<Exercise[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isGuest) return; // the endpoint needs a login
    getExercises()
      .then(setExercises)
      .catch((e: Error) => setError(e.message));
  }, [isGuest]);

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-5xl">
        <header className="px-5 pt-8 pb-2">
          <p className="text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]">Practice</p>
          <h1 className="mt-2 text-5xl font-black tracking-normal text-[#fffdf5]">
            Play along, get scored
          </h1>
          <p className="mt-2 text-base font-semibold text-[#e8f4df]/62">
            Pick a piece, record yourself, and see how your timing and notes compare.
          </p>
        </header>

        <div className="px-5 pt-4">
          {isGuest ? (
            <p className="text-sm font-semibold text-[#e8f4df]/62">
              <Link to="/login" className="text-[#ffd666] underline">
                Log in
              </Link>{" "}
              to practice.
            </p>
          ) : error ? (
            <p className="text-sm text-destructive">{error}</p>
          ) : !exercises ? (
            <p className="text-sm text-[#e8f4df]/62">Loading…</p>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {exercises.map((e) => (
                <ExerciseCard key={e.id} exercise={e} />
              ))}
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}

const instrumentIcons: Record<string, typeof Music2> = { piano: Piano, guitar: Guitar };

function ExerciseCard({ exercise }: { exercise: Exercise }) {
  const Icon = instrumentIcons[exercise.instrument] ?? Music2;

  const card = (
    <Card
      className={cn(
        "relative flex min-h-44 flex-col justify-between border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur",
        exercise.available
          ? "transition-all duration-200 group-hover:-translate-y-0.5 group-hover:border-[#ffd666]/45"
          : "opacity-45",
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#a6eee3]/25 bg-[#a6eee3]/10 text-[#a6eee3]">
          <Icon className="h-5 w-5" />
        </div>
        {!exercise.available && (
          <span className="rounded-full border border-[#fffdf5]/25 px-2.5 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-[#fffdf5]/80">
            {exercise.monophonic ? "Not available yet" : "Coming soon: chord scoring"}
          </span>
        )}
      </div>
      <div className="mt-8">
        <p className="text-lg font-black tracking-tight text-[#fffdf5]">{exercise.title}</p>
        <p className="mt-1 text-sm font-semibold capitalize text-[#e8f4df]/64">
          {exercise.instrument} · {exercise.monophonic ? "single notes" : "chords"}
        </p>
        {exercise.attribution && (
          <p className="mt-3 text-xs text-[#e8f4df]/42">{exercise.attribution}</p>
        )}
      </div>
    </Card>
  );

  // Greyed-out exercises aren't links: there's no reference to score against yet.
  if (!exercise.available) {
    return (
      <div aria-disabled="true" className="cursor-not-allowed">
        {card}
      </div>
    );
  }
  return (
    <Link
      to="/app/practice/$exerciseId"
      params={{ exerciseId: String(exercise.id) }}
      className="group block outline-none"
    >
      {card}
    </Link>
  );
}
