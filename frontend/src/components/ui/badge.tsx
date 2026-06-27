import type * as React from "react";
import { cn } from "../../lib/utils";

const tones: Record<string, string> = {
  ACTIVE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  ADMIN: "bg-purple-50 text-purple-700 ring-purple-200",
  AVAILABLE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  CANCELLED: "bg-zinc-100 text-zinc-600 ring-zinc-200",
  COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  DRAFT: "bg-zinc-100 text-zinc-600 ring-zinc-200",
  HIGH: "bg-orange-50 text-orange-700 ring-orange-200",
  IN_PROGRESS: "bg-sky-50 text-sky-700 ring-sky-200",
  OCCUPIED: "bg-sky-50 text-sky-700 ring-sky-200",
  LANDLORD: "bg-teal-50 text-teal-700 ring-teal-200",
  LOCKED: "bg-red-50 text-red-700 ring-red-200",
  LOW: "bg-zinc-100 text-zinc-600 ring-zinc-200",
  MAINTENANCE: "bg-amber-50 text-amber-700 ring-amber-200",
  MEDIUM: "bg-amber-50 text-amber-700 ring-amber-200",
  OVERDUE: "bg-red-50 text-red-700 ring-red-200",
  PAID: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  PARTIALLY_PAID: "bg-amber-50 text-amber-700 ring-amber-200",
  PENDING: "bg-amber-50 text-amber-700 ring-amber-200",
  TENANT: "bg-sky-50 text-sky-700 ring-sky-200",
  UNPAID: "bg-red-50 text-red-700 ring-red-200",
  INACTIVE: "bg-slate-100 text-slate-600 ring-slate-200",
  URGENT: "bg-red-50 text-red-700 ring-red-200",
};

export function Badge({ className, children, ...props }: React.HTMLAttributes<HTMLSpanElement>) {
  const key = typeof children === "string" ? children : "";
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1",
        tones[key] ?? "bg-slate-100 text-slate-700 ring-slate-200",
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}
