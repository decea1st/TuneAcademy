import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Avatar } from "@/components/tuneacademy/Avatar";
import { Card } from "@/components/tuneacademy/Card";
import {
  formatLevel,
  formatRate,
  getInstructor,
  initials,
  type Instructor,
} from "@/lib/instructors";

export const Route = createFileRoute("/app/instructors/$id")({
  head: () => ({ meta: [{ title: "Instructor - TuneAcademy" }] }),
  component: InstructorPage,
});

function InstructorPage() {
  const id = Number(Route.useParams().id);
  const [instructor, setInstructor] = useState<Instructor | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Works for guests too: the endpoint is public. An unknown id comes back as "Instructor not found."
  useEffect(() => {
    getInstructor(id)
      .then(setInstructor)
      .catch((e: Error) => setError(e.message));
  }, [id]);

  return (
    <AppShell>
      <main className="mx-auto w-full max-w-3xl px-5 pt-6 sm:pt-8">
        <Link
          to="/app/instructors"
          className="mb-4 inline-flex items-center gap-1 text-sm font-semibold text-[#e8f4df]/62 hover:text-[#fffdf5]"
        >
          <ArrowLeft className="h-4 w-4" /> All instructors
        </Link>

        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : !instructor ? (
          <p className="text-sm text-[#e8f4df]/62">Loading…</p>
        ) : (
          <ProfileCard instructor={instructor} />
        )}
      </main>
    </AppShell>
  );
}

function ProfileCard({ instructor }: { instructor: Instructor }) {
  const details = [
    { label: "Experience", value: `${instructor.experienceYears} years` },
    { label: "Nationality", value: instructor.nationality ?? "—" },
    { label: "Teaches", value: instructor.teachingLevels.map(formatLevel).join(", ") },
  ];

  return (
    <Card className="overflow-hidden border-[#fffdf5]/16 bg-[#fffdf5]/8 p-0 shadow-[0_24px_80px_rgba(0,0,0,0.28)] backdrop-blur">
      <div className="border-b border-[#fffdf5]/10 bg-[#0b1510]/40 px-5 py-8 text-center sm:px-8 sm:py-10">
        <Avatar
          initials={initials(instructor.fullName)}
          size={104}
          className="mx-auto text-2xl ring-2 ring-[#ffd666]/35 ring-offset-4 ring-offset-[#0b1510]"
        />
        <h1 className="mt-5 text-3xl font-black tracking-tight text-[#fffdf5]">
          {instructor.fullName}
        </h1>
        <div className="mt-4 flex flex-wrap justify-center gap-1.5">
          {instructor.specialties.map((s) => (
            <span
              key={s}
              className="rounded-full border border-[#a6eee3]/25 bg-[#a6eee3]/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-[#a6eee3]"
            >
              {s}
            </span>
          ))}
        </div>
        <p className="mt-4 text-sm font-black text-[#ffd666]">
          {formatRate(instructor.hourlyRateCents)}
        </p>
      </div>

      <div className="space-y-6 px-5 py-5 sm:px-8 sm:py-7">
        {/* Step 3 adds the "Request lessons" button here. */}

        <dl className="grid gap-3 sm:grid-cols-3">
          {details.map((d) => (
            <div
              key={d.label}
              className="rounded-xl border border-[#fffdf5]/12 bg-[#0b1510]/35 p-3 text-center"
            >
              <dt className="text-[11px] font-black uppercase tracking-[0.14em] text-[#e8f4df]/45">
                {d.label}
              </dt>
              <dd className="mt-1 text-sm font-bold text-[#fffdf5]">{d.value}</dd>
            </div>
          ))}
        </dl>

        <section>
          <h2 className="mb-2 text-sm font-black tracking-tight text-[#fffdf5]">About</h2>
          <div className="rounded-xl border border-[#fffdf5]/12 bg-[#0b1510]/35 p-4 text-sm leading-relaxed text-[#e8f4df]/72">
            {instructor.bio}
          </div>
        </section>
      </div>
    </Card>
  );
}
