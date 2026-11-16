import { apiFetch } from "@/lib/api";
import type { SkillLevel } from "@/lib/profile";

/*
 * Tutoring requests, and the JSON shapes they return.
 * These mirror the records in TutoringController.java.
 */

export type RequestStatus = "PENDING" | "ACCEPTED" | "DECLINED";

/** What an instructor sees. learnerEmail is null until the request is accepted. */
export type IncomingRequest = {
  id: number;
  learnerName: string;
  learnerSkillLevel: SkillLevel | null;
  learnerInstruments: string[];
  message: string;
  status: RequestStatus;
  createdAt: string;
  learnerEmail: string | null;
};

/** What a learner sees about their own request. instructorEmail is null until it's accepted. */
export type MyRequest = {
  id: number;
  instructorId: number;
  instructorName: string;
  message: string;
  status: RequestStatus;
  createdAt: string;
  instructorEmail: string | null;
};

/** Same limit as the backend's @Size(max = 500). */
export const MESSAGE_MAX = 500;

export const sendRequest = (instructorId: number, message: string) =>
  apiFetch<MyRequest>("/api/requests", { method: "POST", body: { instructorId, message } });

export const getIncomingRequests = () => apiFetch<IncomingRequest[]>("/api/requests/incoming");

export const getMyRequests = () => apiFetch<MyRequest[]>("/api/requests/mine");

export const acceptRequest = (id: number) =>
  apiFetch<IncomingRequest>(`/api/requests/${id}/accept`, { method: "POST" });

export const declineRequest = (id: number) =>
  apiFetch<IncomingRequest>(`/api/requests/${id}/decline`, { method: "POST" });
