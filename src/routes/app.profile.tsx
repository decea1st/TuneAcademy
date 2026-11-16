import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowRight, LogOut } from "lucide-react";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Avatar } from "@/components/tuneacademy/Avatar";
import { Card } from "@/components/tuneacademy/Card";
import { Pill } from "@/components/tuneacademy/Pill";
import { useRequireAccount } from "@/components/tuneacademy/RequireAccount";
import { useAuth, type Me } from "@/contexts/AuthContext";
import { formatLevel, initials } from "@/lib/instructors";

export const Route = createFileRoute("/app/profile")({
  head: () => ({ meta: [{ title: "Profile - TuneAcademy" }] }),
  component: ProfilePage,
});

const card = "border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur";
const heading = "text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]";
const muted = "text-sm font-semibold text-[#e8f4df]/62";

/** The logged-in user as GET /api/me returns it (AuthContext loads it on page load). */
function ProfilePage() {
  const { me } = useAuth();
  const { requireAccount } = useRequireAccount();

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-3xl space-y-6 px-5 pt-8">
        <header>
          <p className={heading}>Profile</p>
          <h1 className="mt-2 text-4xl font-black text-[#fffdf5]">My profile</h1>
        </header>

        {me ? (
          <ProfileDetails me={me} />
        ) : (
          <Card className={card}>
            <p className={muted}>
              Your profile holds your name, instruments and skill level. Create a free account to
              set one up.
            </p>
            <Pill className="mt-4" onClick={() => requireAccount()}>
              Create an account
            </Pill>
          </Card>
        )}
      </div>
    </AppShell>
  );
}

function ProfileDetails({ me }: { me: Me }) {
  const nav = useNavigate();
  const { signOutUser } = useAuth();
  const isInstructor = me.role === "INSTRUCTOR";

  async function logOut() {
    await signOutUser();
    void nav({ to: "/", replace: true });
  }

  // Instructors' teaching details (rate, specialties, levels) live on their public listing.
  const details = isInstructor
    ? []
    : [
        { label: "Skill level", value: me.skillLevel ? formatLevel(me.skillLevel) : "—" },
        { label: "Instruments", value: me.instruments.join(", ") || "—" },
      ];

  return (
    <>
      <Card className={card}>
        <div className="flex items-center gap-4">
          <Avatar
            initials={initials(me.fullName)}
            size={72}
            className="ring-2 ring-[#ffd666]/35 ring-offset-2 ring-offset-[#0b1510]"
          />
          <div className="min-w-0">
            <p className="truncate text-2xl font-black text-[#fffdf5]">{me.fullName}</p>
            <p className={`truncate ${muted}`}>{me.email}</p>
            <span className="mt-2 inline-block rounded-full border border-[#a6eee3]/25 bg-[#a6eee3]/10 px-2.5 py-1 text-[11px] font-black uppercase tracking-wide text-[#a6eee3]">
              {isInstructor ? "Instructor" : "Learner"}
            </span>
          </div>
        </div>

        {details.length > 0 && (
          <dl className="mt-5 grid gap-3 sm:grid-cols-2">
            {details.map((d) => (
              <div
                key={d.label}
                className="rounded-xl border border-[#fffdf5]/12 bg-[#0b1510]/35 p-3"
              >
                <dt className="text-[11px] font-black uppercase tracking-[0.14em] text-[#e8f4df]/45">
                  {d.label}
                </dt>
                <dd className="mt-1 text-sm font-bold text-[#fffdf5]">{d.value}</dd>
              </div>
            ))}
          </dl>
        )}

        {me.bio && (
          <section className="mt-5">
            <h2 className={heading}>About</h2>
            <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-[#e8f4df]/72">
              {me.bio}
            </p>
          </section>
        )}

        {isInstructor && (
          <Link
            to="/app/instructors/$id"
            params={{ id: String(me.id) }}
            className="mt-5 inline-flex items-center gap-1 text-sm font-bold text-[#ffd666] hover:underline"
          >
            View my public listing <ArrowRight className="h-4 w-4" />
          </Link>
        )}
      </Card>

      <button
        type="button"
        onClick={() => void logOut()}
        className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-500/40 bg-red-500/12 py-3.5 text-sm font-bold text-red-100 transition hover:bg-red-500/20"
      >
        <LogOut className="h-4 w-4 text-red-200" />
        Log out
      </button>
    </>
  );
}
