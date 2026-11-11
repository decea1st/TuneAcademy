import { cn } from "@/lib/utils";

/**
 * TuneAcademy mark: a "T" whose stem ends in a notehead, on a gold tile.
 * Same geometry as public/favicon.svg — keep the two in sync.
 */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className={cn("h-8 w-8 shrink-0", className)}>
      <rect width="32" height="32" rx="7" fill="#ffd666" />
      <g fill="#0b1510">
        <rect x="7.5" y="5.5" width="18" height="4.5" rx="1.5" />
        <rect x="14.8" y="7" width="3.4" height="14.6" />
        <ellipse cx="13.16" cy="22.4" rx="5.2" ry="3.9" transform="rotate(-22 13.16 22.4)" />
      </g>
    </svg>
  );
}
