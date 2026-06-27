import type * as React from "react";
import { cn } from "../../lib/utils";

export function Select({ className, ...props }: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "h-10 w-full rounded-md border border-zinc-200 bg-white px-3 text-sm outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-100",
        className,
      )}
      {...props}
    />
  );
}
