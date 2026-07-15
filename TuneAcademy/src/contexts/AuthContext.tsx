import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { onAuthStateChanged, signInAnonymously, signOut, type User } from "firebase/auth";
import { getFirebaseAuth } from "@/lib/firebase";
import { getUserDoc, type UserFirestoreDoc } from "@/lib/tuneacademyFirestore";

type AuthState = {
  user: User | null;
  userDoc: UserFirestoreDoc | null;
  /** True while browsing without a real account (anonymous demo session). */
  isGuest: boolean;
  loading: boolean;
  signOutUser: () => Promise<void>;
  refreshUserDoc: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [userDoc, setUserDoc] = useState<UserFirestoreDoc | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUserDoc = useCallback(async () => {
    const auth = getFirebaseAuth();
    const u = auth.currentUser;
    if (!u) {
      setUserDoc(null);
      return;
    }
    setUserDoc(await getUserDoc(u.uid));
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const auth = getFirebaseAuth();
    const unsub = onAuthStateChanged(auth, async (firebaseUser) => {
      if (!firebaseUser) {
        // Demo mode: browse the app with an anonymous session so Firestore
        // reads (which all require auth) keep working. Writes stay gated
        // behind a real account via useRequireAccount.
        setUser(null);
        setUserDoc(null);
        try {
          await signInAnonymously(auth);
          return; // onAuthStateChanged fires again with the anonymous user
        } catch {
          setLoading(false); // offline or provider disabled: fall through logged-out
          return;
        }
      }
      setUser(firebaseUser);
      try {
        setUserDoc(firebaseUser.isAnonymous ? null : await getUserDoc(firebaseUser.uid));
      } catch {
        setUserDoc(null);
      }
      setLoading(false);
    });
    return unsub;
  }, []);

  const signOutUser = useCallback(async () => {
    await signOut(getFirebaseAuth());
    setUserDoc(null);
  }, []);

  const isGuest = !user || user.isAnonymous;

  const value = useMemo(
    () => ({ user, userDoc, isGuest, loading, signOutUser, refreshUserDoc }),
    [user, userDoc, isGuest, loading, signOutUser, refreshUserDoc],
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
