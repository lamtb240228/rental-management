import type * as React from "react";
import { cn } from "../../lib/utils";

export function Textarea({ className, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        "min-h-24 w-full min-w-0 resize-y rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-base outline-none transition placeholder:text-zinc-400 focus:border-teal-500 focus:ring-2 focus:ring-teal-100 sm:rounded-lg sm:text-sm",
        className,
      )}
      {...props}
    />
  );
}
