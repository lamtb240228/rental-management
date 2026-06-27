import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import type * as React from "react";
import { cn } from "../../lib/utils";

const buttonVariants = cva(
  "inline-flex h-10 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold shadow-sm transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-400 disabled:pointer-events-none disabled:opacity-50 hover:-translate-y-0.5",
  {
    variants: {
      variant: {
        default: "bg-teal-600 text-white hover:bg-teal-700",
        secondary: "bg-white text-zinc-900 ring-1 ring-zinc-200 hover:bg-zinc-50",
        ghost: "text-zinc-700 hover:bg-zinc-100",
        danger: "bg-red-600 text-white hover:bg-red-700",
      },
      size: {
        default: "h-10 px-4",
        sm: "h-9 px-3",
        icon: "h-10 w-10 px-0",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

export function Button({ className, variant, size, asChild = false, ...props }: ButtonProps) {
  const Comp = asChild ? Slot : "button";
  return <Comp className={cn(buttonVariants({ variant, size, className }))} {...props} />;
}
