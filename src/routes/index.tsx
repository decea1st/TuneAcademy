import { createFileRoute, Link } from "@tanstack/react-router";
import { LogoMark } from "@/components/tuneacademy/LogoMark";
import { Pill } from "@/components/tuneacademy/Pill";
import { brandTheme, musicImages } from "@/lib/theme";
import { motion } from "framer-motion";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "TuneAcademy - practice and music lessons" },
      {
        name: "description",
        content:
          "Play along with a reference recording, get a score for timing and notes, and request lessons from an instructor.",
      },
      { property: "og:title", content: "TuneAcademy - practice and music lessons" },
      {
        property: "og:description",
        content: "Play along, get a score, and find an instructor.",
      },
    ],
  }),
  component: Welcome,
});

type SampleStatus = "hit" | "wrong" | "missed";

/** A made-up result for the preview card, styled like the real result page. */
const sampleNotes: [string, SampleStatus][] = [
  ["C4", "hit"],
  ["E4", "hit"],
  ["G4", "wrong"],
  ["E4", "hit"],
  ["D4", "missed"],
  ["C4", "hit"],
  ["D4", "hit"],
  ["E4", "hit"],
];

const noteStyle: Record<SampleStatus, string> = {
  hit: "border-emerald-400/60 bg-emerald-500/15 text-emerald-100",
  wrong: "border-amber-400/60 bg-amber-500/15 text-amber-100",
  missed: "border-[#fffdf5]/25 text-[#fffdf5]/50",
};

function Welcome() {
  const notes = ["A", "C#", "F", "G", "Bb", "D", "E"];

  return (
    <div className={`relative min-h-screen overflow-hidden ${brandTheme.page}`}>
      <img
        src={musicImages.studio}
        alt=""
        className="absolute inset-0 h-full w-full object-cover opacity-50"
      />
      <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(8,18,14,0.96)_0%,rgba(8,18,14,0.72)_42%,rgba(8,18,14,0.28)_100%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_75%_18%,rgba(255,214,102,0.32),transparent_30%),radial-gradient(circle_at_78%_78%,rgba(47,197,181,0.28),transparent_32%),radial-gradient(circle_at_16%_70%,rgba(255,88,88,0.18),transparent_26%)]" />

      <div className="pointer-events-none absolute inset-x-0 top-24 hidden h-48 rotate-[-7deg] border-y border-[#fffdf5]/15 opacity-70 md:block">
        <div className="absolute left-0 top-1/4 h-px w-full bg-[#fffdf5]/15" />
        <div className="absolute left-0 top-1/2 h-px w-full bg-[#fffdf5]/15" />
        <div className="absolute left-0 top-3/4 h-px w-full bg-[#fffdf5]/15" />
        {notes.map((note, index) => (
          <motion.span
            key={note}
            className="absolute text-4xl font-black text-[#ffd666]"
            style={{ left: `${14 + index * 12}%`, top: `${index % 2 === 0 ? 18 : 56}%` }}
            animate={{ y: [0, -12, 0], rotate: [-4, 5, -4] }}
            transition={{ duration: 4 + index * 0.35, repeat: Infinity, ease: "easeInOut" }}
          >
            {note}
          </motion.span>
        ))}
      </div>

      <div className="relative mx-auto flex min-h-screen w-full max-w-360 flex-col px-5 py-6 sm:px-8 lg:px-10">
        <header className="flex items-center justify-between">
          <Link
            to="/"
            className={`flex items-center gap-3 text-sm font-black uppercase tracking-[0.24em] ${brandTheme.ink}`}
          >
            <LogoMark />
            TuneAcademy
          </Link>
          <nav className="flex items-center gap-2">
            <Link
              to="/app"
              className="px-3 py-2 text-sm font-semibold text-[#fffdf5] underline underline-offset-4 transition hover:text-[#ffd666]"
            >
              Demo as guest
            </Link>
            <Link
              to="/login"
              className="rounded-lg border border-[#fffdf5]/30 px-4 py-2 text-sm font-semibold text-[#fffdf5] transition hover:border-[#fffdf5] hover:bg-[#fffdf5] hover:text-[#0b1510]"
            >
              Log in
            </Link>
            <Link
              to="/signup"
              search={{ role: "learner" }}
              className="rounded-lg border border-[#ffd666] bg-[#ffd666] px-4 py-2 text-sm font-black text-[#11140c] shadow-[0_14px_36px_rgba(255,214,102,0.24)] transition hover:bg-[#ffe08a]"
            >
              Sign up
            </Link>
          </nav>
        </header>

        <main className="grid flex-1 items-center gap-10 py-12 xl:grid-cols-[minmax(0,1.2fr)_minmax(420px,0.8fr)] xl:py-8">
          <motion.section
            className="max-w-4xl justify-self-center pl-6 sm:pl-12 xl:-mt-25 xl:justify-self-auto xl:pl-0"
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          >
            <p
              className={`mb-5 inline-flex rounded-lg border border-[#ffd666]/40 bg-[#0b1510]/45 px-3 py-2 text-xs font-bold uppercase tracking-[0.22em] ${brandTheme.gold} backdrop-blur`}
            >
              Practice. Score. Learn.
            </p>
            <h1
              className={`max-w-4xl text-5xl font-black leading-[0.88] tracking-normal sm:text-6xl lg:text-7xl ${brandTheme.ink}`}
            >
              Play along.
              <span className={`block ${brandTheme.gold}`}>See your score.</span>
              Find a teacher.
            </h1>
            <p className={`mt-6 max-w-2xl text-lg leading-8 sm:text-xl ${brandTheme.inkSoft}`}>
              Play along with a reference recording and get a score for your timing and notes. When
              you're ready for lessons, send a request to an instructor.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/signup" search={{ role: "learner" }}>
                <Pill size="lg" className={`w-full px-7 sm:w-auto ${brandTheme.primaryButton}`}>
                  Start learning
                </Pill>
              </Link>
              <Link to="/signup" search={{ role: "instructor" }}>
                <Pill
                  size="lg"
                  variant="secondary"
                  className={`w-full px-7 sm:w-auto ${brandTheme.secondaryButton}`}
                >
                  Teach on TuneAcademy
                </Pill>
              </Link>
            </div>

            <Link
              to="/app"
              className={`mt-5 inline-flex items-center gap-1.5 text-sm font-semibold underline-offset-4 transition hover:underline ${brandTheme.inkSoft}`}
            >
              or explore the app without an account
              <span aria-hidden>→</span>
            </Link>
          </motion.section>

          <motion.section
            className="relative hidden min-h-150 w-full max-w-140 justify-self-center xl:block xl:min-h-140 xl:justify-self-end"
            initial={{ opacity: 0, x: 26 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.15, ease: [0.16, 1, 0.3, 1] }}
            aria-label="Sample practice result"
          >
            <div className="absolute left-4 top-8 h-80 w-56 rotate-[-10deg] overflow-hidden rounded-lg border border-[#fffdf5]/20 shadow-[0_30px_80px_rgba(0,0,0,0.35)] sm:left-16">
              <img src={musicImages.piano} alt="" className="h-full w-full object-cover" />
            </div>

            <div
              className={`absolute right-0 top-0 w-full max-w-95 rounded-lg p-5 ${brandTheme.glass}`}
            >
              <div className="flex items-center justify-between text-xs font-bold uppercase tracking-[0.18em] text-[#a6eee3]">
                <span>Practice result</span>
                <span>Score 82</span>
              </div>
              <div className="mt-6 space-y-4">
                {[
                  ["Timing", "88%", "bg-[#ffd666]"],
                  ["Notes", "75%", "bg-[#2fc5b5]"],
                ].map(([label, width, color]) => (
                  <div key={label}>
                    <div className="mb-2 flex justify-between text-sm">
                      <span>{label}</span>
                      <span className="text-[#fffdf5]/70">{width}</span>
                    </div>
                    <div className="h-2 rounded-lg bg-[#fffdf5]/18">
                      <motion.div
                        className={`h-full rounded-lg ${color}`}
                        initial={{ width: 0 }}
                        animate={{ width }}
                        transition={{ duration: 1, delay: 0.45 }}
                      />
                    </div>
                  </div>
                ))}
              </div>
              <div className="mt-6 rounded-lg bg-[#0b1510]/50 p-4">
                <div className="flex flex-wrap gap-1.5">
                  {sampleNotes.map(([note, status], i) => (
                    <span
                      key={i}
                      className={`rounded-md border px-2 py-1 text-xs font-black ${noteStyle[status]}`}
                    >
                      {note}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <div className="absolute right-0 bottom-0 w-full max-w-107.5 rounded-lg border border-[#ffd666]/35 bg-[#ffd666] p-5 text-[#11140c] shadow-[0_24px_70px_rgba(0,0,0,0.32)]">
              <p className="text-xs font-black uppercase tracking-[0.2em]">Request accepted</p>
              <div className="mt-4 flex items-center gap-4">
                <img
                  src={musicImages.singer}
                  alt=""
                  className="h-16 w-16 rounded-lg object-cover"
                />
                <div>
                  <p className="text-xl font-black">Maya Chen</p>
                  <p className="text-sm font-semibold opacity-75">Piano · Beginner, Intermediate</p>
                </div>
              </div>
            </div>
          </motion.section>
        </main>

        <footer className="grid gap-3 border-t border-[#fffdf5]/15 py-5 text-sm text-[#e8f4df]/80 sm:grid-cols-3">
          <p>Record in your browser.</p>
          <p>See which notes you hit, missed or got wrong.</p>
          <p>Request lessons from an instructor.</p>
        </footer>
      </div>
    </div>
  );
}
