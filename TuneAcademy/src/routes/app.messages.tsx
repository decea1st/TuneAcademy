import { createFileRoute } from "@tanstack/react-router";
import { AppShell } from "@/components/tuneacademy/AppShell";
import { MessagesScreen } from "@/components/tuneacademy/MessagesScreen";
import { Pill } from "@/components/tuneacademy/Pill";
import { useRequireAccount } from "@/components/tuneacademy/RequireAccount";
import { useAuth } from "@/contexts/AuthContext";

type Search = { chat?: string };

export const Route = createFileRoute("/app/messages")({
   validateSearch: (s: Record<string, unknown>): Search => ({ chat: typeof s.chat === "string" && s.chat.length > 0 ? s.chat : undefined }),
   head: () => ({ meta: [{ title: "Messages — TuneAcademy" }] }),
   component: MessagesTab,
});

function MessagesTab() {
   const { userDoc, isGuest } = useAuth();
   const { requireAccount } = useRequireAccount();
   const isInstructor = userDoc?.role === "instructor";
   const { chat } = Route.useSearch();

   return (
      <AppShell>
         <header className="pt-8">
            <h1 className="text-3xl font-black tracking-tight text-[#fffdf5]">Messages</h1>
            <p className="mt-2 text-sm leading-relaxed text-[#e8f4df]/70">
               {isInstructor
                  ? "Chat with learners you work with. Threads from MusiReels recruitment show up here for you and the learner."
                  : "Chat with instructors and other students. Use All, Students, or Instructors in the list to focus your threads."}
            </p>
         </header>

         {isGuest ? (
            <div className="mt-10 flex flex-col items-start gap-4 rounded-xl border border-[#fffdf5]/15 bg-[#fffdf5]/4 p-8">
               <p className="max-w-md text-sm leading-relaxed text-[#e8f4df]/80">
                  Messaging is where learners and instructors coordinate lessons, share progress, and follow up on Riffs.
                  Create a free account to start a conversation.
               </p>
               <Pill type="button" onClick={() => requireAccount()}>
                  Create an account
               </Pill>
            </div>
         ) : (
            <MessagesScreen initialChatId={chat} />
         )}
      </AppShell>
   );
}
