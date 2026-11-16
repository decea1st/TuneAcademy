import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { ApiError, apiFetch } from "@/lib/api";

/** What the backend returns for the logged-in user (GET /api/me, signup, login). */
export type Me = {
  id: number;
  email: string;
  role: "LEARNER" | "INSTRUCTOR";
  fullName: string;
  bio: string;
  skillLevel: "BEGINNER" | "INTERMEDIATE" | "ADVANCED" | null;
  instruments: string[];
  onboardingComplete: boolean;
};

export type SignupInput = {
  email: string;
  password: string;
  fullName: string;
  role: Me["role"];
};

/*
 * `user` and `userDoc` keep the shape the Firebase version had (user.uid, userDoc.role, ...)
 * so the ~20 pages that read them keep working while they're migrated one at a time.
 */
type AuthUser = { uid: string; email: string; displayName: string };

type AuthUserDoc = {
  role: "learner" | "instructor";
  fullName: string;
  email: string;
  avatarUrl: string;
  bio: string;
  skillLevel?: "beginner" | "intermediate" | "advanced";
  instruments: string[];
  onboardingComplete: boolean;
  /** Old session-billing fields; that feature is cut, so these are always empty. */
  learningBudgetCapUsd?: number;
  learningBudgetSpentUsd?: number;
};

type AuthState = {
  me: Me | null;
  user: AuthUser | null;
  userDoc: AuthUserDoc | null;
  /** True when nobody is logged in (browsing the app as a guest). */
  isGuest: boolean;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<Me>;
  signUp: (input: SignupInput) => Promise<Me>;
  signOutUser: () => Promise<void>;
  refreshUserDoc: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

/** Where to send someone right after they sign up or log in: anyone without a profile fills it in first. */
export function postLoginPath(me: Me): "/app" | "/onboarding" {
  return me.onboardingComplete ? "/app" : "/onboarding";
}

/** Asks the backend who is logged in. A 401 just means "nobody". */
async function fetchMe(): Promise<Me | null> {
  try {
    return await apiFetch<Me>("/api/me");
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return null;
    throw err;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  // On page load: the browser sends the session cookie, and the backend says who it belongs to.
  useEffect(() => {
    fetchMe()
      .then(setMe)
      .catch(() => setMe(null)) // backend unreachable: treat as logged out
      .finally(() => setLoading(false));
  }, []);

  const refreshUserDoc = useCallback(async () => {
    setMe(await fetchMe());
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const user = await apiFetch<Me>("/api/auth/login", { method: "POST", body: { email, password } });
    setMe(user);
    return user;
  }, []);

  const signUp = useCallback(async (input: SignupInput) => {
    const user = await apiFetch<Me>("/api/auth/signup", { method: "POST", body: input });
    setMe(user);
    return user;
  }, []);

  const signOutUser = useCallback(async () => {
    await apiFetch<void>("/api/auth/logout", { method: "POST" });
    setMe(null);
  }, []);

  const user = useMemo<AuthUser | null>(
    () => (me ? { uid: String(me.id), email: me.email, displayName: me.fullName } : null),
    [me],
  );

  const userDoc = useMemo<AuthUserDoc | null>(
    () =>
      me
        ? {
            role: me.role === "INSTRUCTOR" ? "instructor" : "learner",
            fullName: me.fullName,
            email: me.email,
            avatarUrl: "", // avatar upload is cut; pages fall back to initials
            bio: me.bio,
            skillLevel: me.skillLevel ? (me.skillLevel.toLowerCase() as AuthUserDoc["skillLevel"]) : undefined,
            instruments: me.instruments,
            onboardingComplete: me.onboardingComplete,
          }
        : null,
    [me],
  );

  const value = useMemo(
    () => ({
      me,
      user,
      userDoc,
      isGuest: !me,
      loading,
      signIn,
      signUp,
      signOutUser,
      refreshUserDoc,
    }),
    [me, user, userDoc, loading, signIn, signUp, signOutUser, refreshUserDoc],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
