import type * as React from "react";
import { cn } from "../../lib/utils";

export function Select({ className, ...props }: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "h-11 w-full min-w-0 rounded-xl border border-zinc-200 bg-white px-3 text-base outline-none transition focus:border-teal-500 focus:ring-2 focus:ring-teal-100 sm:h-10 sm:rounded-lg sm:text-sm",
        className,
      )}
      {...props}
    />
  );
}
