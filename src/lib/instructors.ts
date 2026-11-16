import { apiFetch } from "@/lib/api";
import type { SkillLevel } from "@/lib/profile";

/** An instructor as anyone can see them (InstructorResponse.java). There's deliberately no email. */
export type Instructor = {
  id: number;
  fullName: string;
  bio: string;
  experienceYears: number;
  nationality: string | null;
  specialties: string[];
  teachingLevels: SkillLevel[];
  hourlyRateCents: number;
};

export const getInstructors = () => apiFetch<Instructor[]>("/api/instructors");

export const getInstructor = (id: number) => apiFetch<Instructor>(`/api/instructors/${id}`);

/** 4500 → "$45/hr", 4550 → "$45.50/hr", 0 → "Free". */
export function formatRate(cents: number): string {
  if (cents === 0) return "Free";
  const dollars = cents % 100 === 0 ? String(cents / 100) : (cents / 100).toFixed(2);
  return `$${dollars}/hr`;
}

/** "BEGINNER" → "Beginner". */
export function formatLevel(level: SkillLevel): string {
  return level.charAt(0) + level.slice(1).toLowerCase();
}

/** "Maya Chen" → "MC", for the avatar circle (photo upload is cut). */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return (parts[0] ?? "?").slice(0, 2).toUpperCase();
}
