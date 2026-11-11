import { useState } from "react";
import { cn } from "@/lib/utils";

export function Avatar({
  initials,
  src,
  size = 40,
  className,
}: {
  initials: string;
  src?: string;
  size?: number;
  className?: string;
}) {
  const [failed, setFailed] = useState(false);
  const url = src?.trim();
  if (url && !failed) {
    return (
      <div
        className={cn(
          "flex shrink-0 overflow-hidden rounded-full border border-hairline bg-accent",
          className,
        )}
        style={{ width: size, height: size }}
      >
        <img
          src={url}
          alt=""
          className="h-full w-full object-cover rounded-[inherit]"
          onError={() => setFailed(true)}
        />
      </div>
    );
  }
  return (
    <div
      className={cn(
        "flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-hairline bg-accent font-semibold tracking-tight",
        className,
      )}
      style={{ width: size, height: size, fontSize: size * 0.36 }}
    >
      {initials}
    </div>
  );
}
