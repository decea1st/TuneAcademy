import { createFileRoute, Link } from "@tanstack/react-router";
import { Copy } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { Card } from "@/components/tuneacademy/Card";
import { Pill } from "@/components/tuneacademy/Pill";
import { useAuth } from "@/contexts/AuthContext";
import { formatLevel } from "@/lib/instructors";
import {
  acceptRequest,
  declineRequest,
  getIncomingRequests,
  getMyRequests,
  type IncomingRequest,
  type MyRequest,
  type RequestStatus,
} from "@/lib/requests";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/requests")({
  head: () => ({ meta: [{ title: "Requests - TuneAcademy" }] }),
  component: RequestsPage,
});

const card = "border-[#fffdf5]/16 bg-[#fffdf5]/8 p-5 backdrop-blur";
const heading = "text-xs font-black uppercase tracking-[0.22em] text-[#a6eee3]";
const muted = "text-sm font-semibold text-[#e8f4df]/62";

/** One page for both roles: instructors answer requests, learners follow the ones they sent. */
function RequestsPage() {
  const { me } = useAuth();
  const isInstructor = me?.role === "INSTRUCTOR";

  return (
    <AppShell>
      <div className="mx-auto w-full max-w-3xl space-y-6 px-5 pt-8">
        <header>
          <p className={heading}>Requests</p>
          <h1 className="mt-2 text-4xl font-black text-[#fffdf5]">
            {isInstructor ? "Lesson requests" : "My lesson requests"}
          </h1>
          <p className={`mt-2 ${muted}`}>
            {isInstructor
              ? "Accepting a request shares your email address with that learner, and theirs with you."
              : "When an instructor accepts, you'll see their email address here."}
          </p>
        </header>

        {!me ? (
          <p className={muted}>
            <Link to="/login" className="text-[#ffd666] underline">
              Log in
            </Link>{" "}
            to see your requests.
          </p>
        ) : isInstructor ? (
          <InstructorRequests />
        ) : (
          <LearnerRequests />
        )}
      </div>
    </AppShell>
  );
}

// ---- Instructor: pending (accept / decline), then accepted (with the learner's email) ----

function InstructorRequests() {
  const [requests, setRequests] = useState<IncomingRequest[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    getIncomingRequests()
      .then(setRequests)
      .catch((e: Error) => setError(e.message));
  }, []);

  async function answer(id: number, send: (id: number) => Promise<IncomingRequest>) {
    setBusyId(id);
    try {
      const updated = await send(id);
      // Accepted: swap in the updated request, which now carries the email. Declined: it leaves the list.
      setRequests((list) =>
        (list ?? []).flatMap((r) =>
          r.id !== id ? [r] : updated.status === "DECLINED" ? [] : [updated],
        ),
      );
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Something went wrong.");
    } finally {
      setBusyId(null);
    }
  }

  if (error) return <p className="text-sm text-destructive">{error}</p>;
  if (!requests) return <p className={muted}>Loading…</p>;

  const pending = requests.filter((r) => r.status === "PENDING");
  const accepted = requests.filter((r) => r.status === "ACCEPTED");

  return (
    <>
      <Section title={`Waiting for you (${pending.length})`} empty="No requests waiting.">
        {pending.map((r) => (
          <RequestRow key={r.id} title={r.learnerName} subtitle={learnerDetails(r)} request={r}>
            <Pill
              size="sm"
              className="bg-[#ffd666] text-[#0b1510]"
              disabled={busyId === r.id}
              onClick={() => void answer(r.id, acceptRequest)}
            >
              Accept
            </Pill>
            <Pill
              size="sm"
              variant="ghost"
              disabled={busyId === r.id}
              onClick={() => void answer(r.id, declineRequest)}
            >
              Decline
            </Pill>
          </RequestRow>
        ))}
      </Section>

      <Section title={`Accepted (${accepted.length})`} empty="No accepted requests yet.">
        {accepted.map((r) => (
          <RequestRow key={r.id} title={r.learnerName} subtitle={learnerDetails(r)} request={r}>
            {r.learnerEmail && <ContactEmail email={r.learnerEmail} />}
          </RequestRow>
        ))}
      </Section>
    </>
  );
}

function learnerDetails(r: IncomingRequest): string {
  const level = r.learnerSkillLevel ? formatLevel(r.learnerSkillLevel) : null;
  return [level, r.learnerInstruments.join(", ")].filter(Boolean).join(" · ");
}

// ---- Learner: every request they sent, with its status (and the instructor's email once accepted) ----

function LearnerRequests() {
  const [requests, setRequests] = useState<MyRequest[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMyRequests()
      .then(setRequests)
      .catch((e: Error) => setError(e.message));
  }, []);

  if (error) return <p className="text-sm text-destructive">{error}</p>;
  if (!requests) return <p className={muted}>Loading…</p>;
  if (requests.length === 0) {
    return (
      <p className={muted}>
        No requests yet.{" "}
        <Link to="/app/instructors" className="text-[#ffd666] underline">
          Find an instructor
        </Link>
      </p>
    );
  }

  return (
    <Section title={`Sent (${requests.length})`} empty="">
      {requests.map((r) => (
        <RequestRow key={r.id} title={r.instructorName} request={r}>
          <StatusBadge status={r.status} />
          {r.instructorEmail && <ContactEmail email={r.instructorEmail} />}
        </RequestRow>
      ))}
    </Section>
  );
}

// ---- Shared pieces ----

function Section({
  title,
  empty,
  children,
}: {
  title: string;
  empty: string;
  children: ReactNode[];
}) {
  return (
    <Card className={card}>
      <h2 className={heading}>{title}</h2>
      {children.length === 0 ? (
        <p className={`mt-2 ${muted}`}>{empty}</p>
      ) : (
        <ul className="mt-3 divide-y divide-[#fffdf5]/10">{children}</ul>
      )}
    </Card>
  );
}

function RequestRow({
  title,
  subtitle,
  request,
  children,
}: {
  title: string;
  subtitle?: string;
  request: { message: string; createdAt: string };
  children: ReactNode;
}) {
  return (
    <li className="space-y-2 py-4">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-base font-black text-[#fffdf5]">{title}</p>
        <p className="text-xs text-[#e8f4df]/42">
          {new Date(request.createdAt).toLocaleDateString()}
        </p>
      </div>
      {subtitle && <p className="text-xs font-semibold text-[#a6eee3]">{subtitle}</p>}
      <p className="whitespace-pre-line text-sm text-[#e8f4df]/72">{request.message}</p>
      <div className="flex flex-wrap items-center gap-2 pt-1">{children}</div>
    </li>
  );
}

const statusStyle: Record<RequestStatus, { label: string; className: string }> = {
  PENDING: { label: "Pending", className: "border-amber-400/60 bg-amber-500/15 text-amber-100" },
  ACCEPTED: {
    label: "Accepted",
    className: "border-emerald-400/60 bg-emerald-500/15 text-emerald-100",
  },
  DECLINED: { label: "Declined", className: "border-[#fffdf5]/25 text-[#fffdf5]/50" },
};

function StatusBadge({ status }: { status: RequestStatus }) {
  return (
    <span
      className={cn(
        "rounded-full border px-2.5 py-1 text-[11px] font-black uppercase tracking-wide",
        statusStyle[status].className,
      )}
    >
      {statusStyle[status].label}
    </span>
  );
}

/** Shows the other person's email (only sent once a request is accepted) with a "Copy email" button. */
function ContactEmail({ email }: { email: string }) {
  async function copy() {
    try {
      await navigator.clipboard.writeText(email);
      toast.success("Email copied");
    } catch {
      toast.error("Couldn't copy. Select the address and copy it instead.");
    }
  }

  return (
    <>
      <span className="text-sm font-semibold text-[#fffdf5]">{email}</span>
      <Pill size="sm" variant="ghost" onClick={() => void copy()}>
        <Copy className="h-3.5 w-3.5" /> Copy email
      </Pill>
    </>
  );
}
