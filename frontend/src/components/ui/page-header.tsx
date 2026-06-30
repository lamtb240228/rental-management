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
    <div className={cn("rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:p-5", className)}>
      <div className="flex flex-col items-stretch justify-between gap-4 sm:flex-row sm:items-start">
        <div className="min-w-0">
          {subtitle && <p className="text-sm font-medium text-teal-700">{subtitle}</p>}
          <h1 className="mt-1 break-words text-xl font-semibold leading-tight text-zinc-950 sm:text-3xl">{title}</h1>
          {description && <p className="mt-3 max-w-3xl text-sm leading-6 text-zinc-500 sm:mt-4">{description}</p>}
        </div>
        {action && <div className="w-full shrink-0 [&>*]:w-full sm:w-auto sm:[&>*]:w-auto">{action}</div>}
      </div>
    </div>
  );
}
