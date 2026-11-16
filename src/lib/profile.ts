import { apiFetch } from "@/lib/api";
import type { Me } from "@/contexts/AuthContext";

export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

/**
 * What PUT /api/me/profile takes (ProfileController.ProfileRequest).
 * Learners send the learner fields, instructors the instructor fields.
 */
export type ProfileInput = {
  fullName: string;
  bio?: string;
  // Learners
  skillLevel?: SkillLevel;
  instruments?: string[];
  // Instructors
  experienceYears?: number;
  nationality?: string;
  specialties?: string[];
  teachingLevels?: SkillLevel[];
  /** Whole cents, e.g. $45.00 is 4500. */
  hourlyRateCents?: number;
};

/** Saves the onboarding form; the answer is the updated logged-in user. */
export const saveProfile = (input: ProfileInput) =>
  apiFetch<Me>("/api/me/profile", { method: "PUT", body: input });
