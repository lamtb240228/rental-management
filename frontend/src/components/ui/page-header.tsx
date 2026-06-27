import type { ReactNode } from "react";
import { cn } from "../../lib/utils";

type PageHeaderProps = {
  title: string;
  subtitle?: string;
  description?: string;
  action?: ReactNode;
  className?: string;
};

export function PageHeader({ title, subtitle, description, action, className }: PageHeaderProps) {
  return (
    <div className={cn("mb-6 rounded-3xl border border-slate-200 bg-white/95 p-5 shadow-sm", className)}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          {subtitle && <p className="text-sm font-medium text-teal-700">{subtitle}</p>}
          <h1 className="mt-1 text-2xl font-semibold text-zinc-950 sm:text-3xl">{title}</h1>
        </div>
        {action}
      </div>
      {description && <p className="mt-4 max-w-3xl text-sm text-zinc-500">{description}</p>}
    </div>
  );
}
