import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, Inbox, Mic, Users } from "lucide-react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Card } from "@/components/tuneacademy/Card";
import { useAuth } from "@/contexts/AuthContext";

export const Route = createFileRoute("/app/")({
  head: () => ({ meta: [{ title: "Home - TuneAcademy" }] }),
  component: HomePage,
});

const heading = "text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]";
const muted = "text-sm font-semibold text-[#e8f4df]/62";

function HomePage() {
  const { me } = useAuth();
  const firstName = me?.fullName.trim().split(/\s+/)[0];
  const isInstructor = me?.role === "INSTRUCTOR";

  const links = [
    {
      to: "/app/practice",
      title: "Practice",
      icon: Mic,
      text: "Play along with a piece and get scored on timing and notes.",
    },
    {
      to: "/app/instructors",
      title: "Instructors",
      icon: Users,
      text: isInstructor
        ? "Browse the instructor directory."
        : "Browse instructors and request lessons.",
    },
    {
      to: "/app/requests",
      title: "Requests",
      icon: Inbox,
      text: isInstructor
        ? "Accept or decline lesson requests from learners."
        : "Follow the lesson requests you've sent.",
    },
  ] as const;

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-5xl space-y-6 px-5 pt-8">
        <header>
          <p className={heading}>TuneAcademy</p>
          <h1 className="mt-2 text-5xl font-black tracking-normal text-[#fffdf5]">
            {firstName ? `Welcome back, ${firstName}` : "Welcome to TuneAcademy"}
          </h1>
          <p className={`mt-2 ${muted}`}>Where would you like to start?</p>
        </header>

        <div className="grid gap-4 sm:grid-cols-3">
          {links.map(({ to, title, icon: Icon, text }) => (
            <Link key={to} to={to} className="group block outline-none">
              <Card className="flex h-full flex-col gap-6 border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur transition-all duration-200 group-hover:-translate-y-0.5 group-hover:border-[#ffd666]/45">
                <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#a6eee3]/25 bg-[#a6eee3]/10 text-[#a6eee3]">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="flex items-center gap-2 text-lg font-black tracking-tight text-[#fffdf5]">
                    {title}
                    <ArrowRight className="h-4 w-4 text-[#ffd666] transition-transform group-hover:translate-x-0.5" />
                  </p>
                  <p className={`mt-1 ${muted}`}>{text}</p>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
