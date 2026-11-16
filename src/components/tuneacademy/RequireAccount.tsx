import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { useNavigate } from "@tanstack/react-router";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/AuthContext";

type RequireAccountState = {
   /**
    * Runs `action` if the visitor has a real account; otherwise opens a
    * "create an account" dialog and does nothing. Omit `action` to use it
    * as a plain boolean check: `if (!requireAccount()) return;`
    */
   requireAccount: (action?: () => void) => boolean;
};

const RequireAccountContext = createContext<RequireAccountState | null>(null);

export function RequireAccountProvider({ children }: { children: ReactNode }) {
   const { isGuest } = useAuth();
   const nav = useNavigate();
   const [open, setOpen] = useState(false);

   const requireAccount = useCallback(
      (action?: () => void) => {
         if (isGuest) {
            setOpen(true);
            return false;
         }
         action?.();
         return true;
      },
      [isGuest],
   );

   return (
      <RequireAccountContext.Provider value={{ requireAccount }}>
         {children}
         <Dialog open={open} onOpenChange={setOpen}>
            <DialogContent className="max-w-sm">
               <DialogHeader>
                  <DialogTitle>Create an account to continue</DialogTitle>
                  <DialogDescription>
                     You are browsing TuneAcademy as a guest. Requesting lessons needs a free account.
                  </DialogDescription>
               </DialogHeader>
               <DialogFooter className="gap-2 sm:gap-2">
                  <Button variant="secondary" onClick={() => setOpen(false)}>
                     Keep browsing
                  </Button>
                  <Button variant="outline" onClick={() => void nav({ to: "/login" })}>
                     Log in
                  </Button>
                  <Button onClick={() => void nav({ to: "/signup" })}>Sign up</Button>
               </DialogFooter>
            </DialogContent>
         </Dialog>
      </RequireAccountContext.Provider>
   );
}

export function useRequireAccount(): RequireAccountState {
   const ctx = useContext(RequireAccountContext);
   if (!ctx) throw new Error("useRequireAccount must be used within RequireAccountProvider");
   return ctx;
}
