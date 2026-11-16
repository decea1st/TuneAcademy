import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Avatar } from "@/components/tuneacademy/Avatar";
import { Card } from "@/components/tuneacademy/Card";
import {
  formatLevel,
  formatRate,
  getInstructors,
  initials,
  type Instructor,
} from "@/lib/instructors";

export const Route = createFileRoute("/app/instructors/")({
  head: () => ({ meta: [{ title: "Instructors - TuneAcademy" }] }),
  component: InstructorsTab,
});

const messageCard =
  "col-span-full border-[#fffdf5]/16 bg-[#fffdf5]/8 p-8 text-center text-base text-[#e8f4df]/62 backdrop-blur";

function InstructorsTab() {
  const [instructors, setInstructors] = useState<Instructor[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Works for guests too: the endpoint is public.
  useEffect(() => {
    getInstructors()
      .then(setInstructors)
      .catch((e: Error) => setError(e.message));
  }, []);

  return (
    <AppShell>
      <header className="px-5 pb-4 pt-8">
        <h1 className="text-3xl font-black tracking-tight text-[#fffdf5] sm:text-4xl">
          Find your instructor
        </h1>
        <p className="mt-2 text-base font-semibold text-[#e8f4df]/62 sm:text-lg">
          Browse instructors and request lessons with the one that fits you.
        </p>
      </header>

      <div className="grid grid-cols-2 gap-4 px-5 pt-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {error ? (
          <Card className={messageCard}>{error}</Card>
        ) : !instructors ? (
          <Card className={messageCard}>Loading instructors...</Card>
        ) : instructors.length === 0 ? (
          <Card className={messageCard}>No instructors yet.</Card>
        ) : (
          instructors.map((instructor) => (
            <InstructorCard key={instructor.id} instructor={instructor} />
          ))
        )}
      </div>
    </AppShell>
  );
}

function InstructorCard({ instructor }: { instructor: Instructor }) {
  return (
    <Link
      to="/app/instructors/$id"
      params={{ id: String(instructor.id) }}
      className="group block h-full min-h-0 rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-[#ffd666]/70 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0b1510]"
    >
      <Card className="flex h-full min-h-70 flex-col overflow-hidden border-[#fffdf5]/16 bg-[#fffdf5]/8 p-0 shadow-[0_20px_60px_rgba(0,0,0,0.18)] backdrop-blur transition-all duration-200 hover:border-[#ffd666]/45 hover:bg-[#fffdf5]/10 hover:shadow-[0_24px_70px_rgba(0,0,0,0.26)] active:scale-[0.99] sm:min-h-[19rem]">
        <div className="relative aspect-square w-full shrink-0 border-b border-[#fffdf5]/10 bg-[#0b1510]/35">
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_20%,rgba(255,214,102,0.14),transparent_50%)]" />
          <div className="absolute inset-0 flex items-center justify-center p-1.5 sm:p-2">
            <Avatar
              initials={initials(instructor.fullName)}
              size={156}
              className="!rounded-[2.75rem] ring-2 ring-[#ffd666]/35 ring-offset-2 ring-offset-[#0b1510]/70 transition-transform duration-200 group-hover:scale-[1.02] sm:!rounded-[3.25rem]"
            />
          </div>
        </div>

        <div className="flex min-h-0 flex-1 flex-col gap-1.5 p-3 pt-2.5">
          <div>
            <p className="line-clamp-2 text-base font-black leading-snug tracking-tight text-[#fffdf5] sm:text-lg">
              {instructor.fullName}
            </p>
            <p className="mt-1 text-sm font-bold tabular-nums text-[#ffd666] sm:text-base">
              {formatRate(instructor.hourlyRateCents)}
            </p>
          </div>

          <div className="flex flex-wrap gap-1">
            {instructor.specialties.map((specialty) => (
              <span
                key={specialty}
                className="rounded-full border border-[#a6eee3]/25 bg-[#a6eee3]/10 px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-[#a6eee3] sm:px-2.5 sm:text-[11px]"
              >
                {specialty}
              </span>
            ))}
          </div>

          <div className="flex flex-wrap gap-1">
            {instructor.teachingLevels.map((level) => (
              <span
                key={level}
                className="rounded-full border border-[#fffdf5]/14 bg-[#fffdf5]/6 px-2 py-0.5 text-[10px] font-bold tracking-wide text-[#e8f4df]/75 sm:px-2.5 sm:text-[11px]"
              >
                {formatLevel(level)}
              </span>
            ))}
          </div>

          <p className="line-clamp-3 flex-1 text-xs leading-relaxed text-[#e8f4df]/62 sm:text-sm sm:leading-relaxed">
            {instructor.bio}
          </p>
        </div>
      </Card>
    </Link>
  );
}
