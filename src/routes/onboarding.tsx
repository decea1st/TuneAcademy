import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Chip } from "@/components/tuneacademy/Chip";
import { Pill } from "@/components/tuneacademy/Pill";
import { useAuth } from "@/contexts/AuthContext";
import { NATIONALITY_OPTIONS } from "@/lib/nationalityOptions";
import { saveProfile, type SkillLevel } from "@/lib/profile";

export const Route = createFileRoute("/onboarding")({
  head: () => ({ meta: [{ title: "Set up your profile — TuneAcademy" }] }),
  component: Onboarding,
});

const INSTRUMENTS = ["Voice", "Guitar", "Piano", "Saxophone", "Violin", "Drums", "Bass", "Other"];
const LEVELS: { value: SkillLevel; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "ADVANCED", label: "Advanced" },
];

const input =
  "h-12 w-full rounded-xl border border-hairline bg-surface px-4 text-sm outline-none focus:border-foreground";
const fieldLabel = "mt-6 mb-2 text-[11px] uppercase tracking-widest text-muted-foreground";

/**
 * Learners: name → skill level + instruments.
 * Instructors: name → teaching details → bio.
 * Everything is sent in one request at the end.
 */
function Onboarding() {
  const nav = useNavigate();
  const { me, loading, refreshUserDoc } = useAuth();
  const [step, setStep] = useState(0);
  const [saving, setSaving] = useState(false);

  const [name, setName] = useState("");
  const [bio, setBio] = useState("");
  // Learners
  const [skillLevel, setSkillLevel] = useState<SkillLevel | null>(null);
  const [instruments, setInstruments] = useState<string[]>([]);
  // Instructors
  const [years, setYears] = useState("");
  const [nationality, setNationality] = useState("");
  const [rate, setRate] = useState("");
  const [specialties, setSpecialties] = useState<string[]>([]);
  const [teachingLevels, setTeachingLevels] = useState<SkillLevel[]>([]);

  // Not logged in: log in first. Already onboarded: nothing to do here.
  useEffect(() => {
    if (loading) return;
    if (!me) void nav({ to: "/login", replace: true });
    else if (me.onboardingComplete) void nav({ to: "/app", replace: true });
  }, [loading, me, nav]);

  // Start with the name they signed up with.
  useEffect(() => {
    if (me) setName((prev) => prev || me.fullName);
  }, [me]);

  if (loading || !me) return null;
  const isLearner = me.role === "LEARNER";
  const lastStep = isLearner ? 1 : 2;

  /** What's missing on the current step, if anything. The backend checks the same rules. */
  function stepError(): string | null {
    if (step === 0) return name.trim() ? null : "Enter your name.";
    if (isLearner) {
      if (!skillLevel) return "Select your skill level.";
      if (instruments.length === 0) return "Pick at least one instrument.";
      return null;
    }
    if (step === 1) {
      if (!/^\d+$/.test(years.trim())) return "Enter your years of experience.";
      if (!nationality) return "Choose your nationality.";
      if (!/^\d+(\.\d{1,2})?$/.test(rate.trim()))
        return "Enter your hourly rate, like 45 or 45.50.";
      if (specialties.length === 0) return "Pick at least one specialty.";
      if (teachingLevels.length === 0) return "Pick at least one teaching level.";
      return null;
    }
    return bio.trim() ? null : "Add a short bio.";
  }

  async function onContinue() {
    const error = stepError();
    if (error) {
      toast.error(error);
      return;
    }
    if (step < lastStep) {
      setStep(step + 1);
      return;
    }

    setSaving(true);
    try {
      await saveProfile(
        isLearner
          ? { fullName: name.trim(), skillLevel: skillLevel ?? undefined, instruments }
          : {
              fullName: name.trim(),
              bio: bio.trim(),
              experienceYears: Number(years),
              nationality,
              specialties,
              teachingLevels,
              hourlyRateCents: Math.round(Number(rate) * 100), // dollars → whole cents
            },
      );
      await refreshUserDoc(); // the app now sees onboardingComplete = true
      toast.success("Profile saved");
      void nav({ to: "/app", replace: true });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not save profile.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col px-6 py-8">
      {/* Progress dots */}
      <div className="flex items-center justify-center gap-2">
        {Array.from({ length: lastStep + 1 }).map((_, i) => (
          <div
            key={i}
            className={
              "h-1.5 w-8 rounded-full transition-colors sm:w-10 " +
              (i <= step ? "bg-foreground" : "bg-accent")
            }
          />
        ))}
      </div>

      <div className="mt-10 flex-1">
        {step === 0 && (
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Your profile</h2>
            <p className="mt-1 text-sm text-muted-foreground">How should people see your name?</p>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Full name *"
              className={`mt-8 ${input}`}
            />
          </div>
        )}

        {step === 1 && isLearner && (
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Your music journey</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Instructors see this when you request lessons.
            </p>
            <p className={fieldLabel}>Skill level *</p>
            <div className="flex gap-3">
              {LEVELS.map((level) => (
                <Chip
                  key={level.value}
                  active={skillLevel === level.value}
                  onClick={() => setSkillLevel(level.value)}
                >
                  {level.label}
                </Chip>
              ))}
            </div>
            <p className={fieldLabel}>Instruments *</p>
            <div className="flex flex-wrap gap-2">
              {INSTRUMENTS.map((inst) => (
                <Chip
                  key={inst}
                  active={instruments.includes(inst)}
                  onClick={() => setInstruments((v) => toggle(v, inst))}
                >
                  {inst}
                </Chip>
              ))}
            </div>
          </div>
        )}

        {step === 1 && !isLearner && (
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Your details</h2>
            <p className="mt-1 text-sm text-muted-foreground">Tell learners what you teach.</p>
            <div className="mt-6 grid grid-cols-2 gap-3">
              <input
                value={years}
                onChange={(e) => setYears(e.target.value)}
                inputMode="numeric"
                placeholder="Years experience *"
                className={input}
              />
              <input
                value={rate}
                onChange={(e) => setRate(e.target.value)}
                inputMode="decimal"
                placeholder="Hourly rate ($) *"
                className={input}
              />
            </div>
            <select
              value={nationality}
              onChange={(e) => setNationality(e.target.value)}
              className={`mt-3 cursor-pointer text-foreground ${input}`}
            >
              <option value="">Select nationality… *</option>
              {NATIONALITY_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>

            <p className={fieldLabel}>Specialties *</p>
            <div className="flex flex-wrap gap-2">
              {INSTRUMENTS.map((s) => (
                <Chip
                  key={s}
                  active={specialties.includes(s)}
                  onClick={() => setSpecialties((v) => toggle(v, s))}
                >
                  {s}
                </Chip>
              ))}
            </div>

            <p className={fieldLabel}>Levels you teach *</p>
            <div className="flex flex-wrap gap-2">
              {LEVELS.map((level) => (
                <Chip
                  key={level.value}
                  active={teachingLevels.includes(level.value)}
                  onClick={() => setTeachingLevels((v) => toggle(v, level.value))}
                >
                  {level.label}
                </Chip>
              ))}
            </div>
          </div>
        )}

        {step === 2 && !isLearner && (
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Short bio</h2>
            <p className="mt-1 text-sm text-muted-foreground">Up to 300 characters.</p>
            <textarea
              value={bio}
              maxLength={300}
              onChange={(e) => setBio(e.target.value)}
              rows={6}
              placeholder="What's your teaching style? *"
              className="mt-6 w-full resize-none rounded-xl border border-hairline bg-surface p-4 text-sm outline-none focus:border-foreground"
            />
            <p className="mt-1 text-right text-[11px] text-muted-foreground">{bio.length}/300</p>
          </div>
        )}
      </div>

      <div className="flex gap-3">
        {step > 0 && (
          <Pill size="lg" variant="ghost" disabled={saving} onClick={() => setStep(step - 1)}>
            Back
          </Pill>
        )}
        <Pill size="lg" className="flex-1" disabled={saving} onClick={() => void onContinue()}>
          {saving ? "Saving…" : step === lastStep ? "Complete profile" : "Continue"}
        </Pill>
      </div>
    </div>
  );
}

/** Adds the item if it's missing, removes it if it's there. */
function toggle<T>(list: T[], item: T): T[] {
  return list.includes(item) ? list.filter((x) => x !== item) : [...list, item];
}
