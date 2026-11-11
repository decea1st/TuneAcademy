import { createFileRoute, Outlet, useNavigate } from "@tanstack/react-router";
import { AppNav } from "@/components/tuneacademy/AppNav";
import { ChatbotFab } from "@/components/tuneacademy/ChatbotFab";
import { useAuth } from "@/contexts/AuthContext";
import { RequireAccountProvider } from "@/components/tuneacademy/RequireAccount";
import { getInstructorDoc, instructorOnboardingComplete } from "@/lib/tuneacademyFirestore";
import { useEffect, useState } from "react";
import { Pill } from "@/components/tuneacademy/Pill";

export const Route = createFileRoute("/app")({ component: AppLayout });

function AppLayout() {
   const nav = useNavigate();
   const { user, userDoc, isGuest, loading, signOutUser } = useAuth();
   const [instructorCheck, setInstructorCheck] = useState<"pending" | "ready">("pending");

   useEffect(() => {
      if (loading) return;
      if (isGuest) {
         // Demo mode: guests browse everything; writes are gated by useRequireAccount.
         setInstructorCheck("ready");
         return;
      }
      if (!user) return;
      if (!userDoc || userDoc.role !== "instructor") {
         setInstructorCheck("ready");
         return;
      }
      void getInstructorDoc(user.uid).then((inst) => {
         if (instructorOnboardingComplete(inst)) setInstructorCheck("ready");
         else void nav({ to: "/onboarding", replace: true });
      });
   }, [loading, isGuest, user, userDoc, nav]);

   if (loading) {
      return <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">Loading…</div>;
   }

   if (!isGuest && !userDoc) {
      return (
         <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background px-6 text-center">
            <p className="max-w-sm text-sm text-muted-foreground">
               Your account exists, but no TuneAcademy profile was found in Firestore. Sign out and sign up again, or check your Firebase
               project and network connection.
            </p>
            <Pill
               type="button"
               variant="secondary"
               className="border-red-500/50 bg-red-500/12 text-red-100 hover:bg-red-500/20 hover:text-red-50"
               onClick={() => void signOutUser()}
            >
               Sign out
            </Pill>
         </div>
      );
   }

   if (!isGuest && userDoc && userDoc.role === "instructor" && instructorCheck === "pending") {
      return <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">Loading…</div>;
   }

   return (
      <RequireAccountProvider>
         <div className="min-h-screen bg-background text-foreground">
            <Outlet />
            <ChatbotFab />
            <AppNav />
         </div>
      </RequireAccountProvider>
   );
}
