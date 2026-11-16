import { createFileRoute, Outlet, useNavigate } from "@tanstack/react-router";
import { useEffect } from "react";
import { AppNav } from "@/components/tuneacademy/AppNav";
import { ChatbotFab } from "@/components/tuneacademy/ChatbotFab";
import { RequireAccountProvider } from "@/components/tuneacademy/RequireAccount";
import { useAuth } from "@/contexts/AuthContext";

export const Route = createFileRoute("/app")({ component: AppLayout });

function AppLayout() {
  const nav = useNavigate();
  const { me, loading } = useAuth();
  // Logged in but the profile isn't filled in yet. Guests (me = null) can still browse.
  const needsOnboarding = me !== null && !me.onboardingComplete;

  useEffect(() => {
    if (needsOnboarding) void nav({ to: "/onboarding", replace: true });
  }, [needsOnboarding, nav]);

  if (loading || needsOnboarding) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Loading…
      </div>
    );
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
