import {
  Building2,
  FileText,
  Home,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  Gauge,
  ShieldCheck,
  UserCog,
  Users,
  Wrench,
  X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Button } from "../../components/ui/button";
import { useAuth } from "../../features/auth/AuthProvider";
import { cn } from "../../lib/utils";

type NavItem = {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
};

const landlordNavItems: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/properties", label: "Khu trọ", icon: Home },
  { to: "/tenants", label: "Người thuê", icon: Users },
  { to: "/contracts", label: "Hợp đồng", icon: FileText },
  { to: "/utilities", label: "Điện nước", icon: Gauge },
  { to: "/invoices", label: "Hóa đơn", icon: ReceiptText },
  { to: "/maintenance", label: "Sửa chữa", icon: Wrench },
];

const adminNavItems: NavItem[] = [
  { to: "/", label: "Tổng quan", icon: ShieldCheck },
  { to: "/admin/users", label: "Tài khoản", icon: UserCog },
];

const tenantNavItems: NavItem[] = [
  { to: "/", label: "Cổng thuê", icon: Home },
];

export function AppLayout() {
  const { user, signOut } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navItems = getNavItems(user?.roles ?? []);
  const primaryRole = getPrimaryRole(user?.roles ?? []);

  useEffect(() => {
    if (!mobileOpen) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMobileOpen(false);
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", closeOnEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [mobileOpen]);

  return (
    <div className="min-h-screen bg-slate-50 text-zinc-900">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-800 bg-slate-900/95 text-white shadow-2xl lg:block">
        <div className="px-5 py-5">
          <Brand />
          <p className="mt-3 text-sm text-slate-300">Quản lý thuê trọ, hợp đồng, hóa đơn và bảo trì dễ dàng.</p>
        </div>
        <div className="border-t border-white/10" />
        <SidebarContent navItems={navItems} onNavigate={() => setMobileOpen(false)} compactBrand />
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button className="absolute inset-0 bg-slate-950/70 backdrop-blur-sm" aria-label="Đóng menu" onClick={() => setMobileOpen(false)} />
          <aside className="relative h-full w-[min(20rem,calc(100vw-2rem))] overflow-y-auto border-r border-white/10 bg-slate-900/95 text-white shadow-2xl">
            <div className="border-b border-white/10 px-4 py-4">
              <div className="flex items-center justify-between gap-3">
                <Brand />
                <Button variant="ghost" size="icon" className="text-white hover:bg-white/10" onClick={() => setMobileOpen(false)}>
                  <X className="h-5 w-5" />
                </Button>
              </div>
              <div className="mt-4 space-y-1 text-slate-300">
                <p className="truncate text-sm font-semibold text-white">{user?.fullName}</p>
                <p className="truncate text-xs">{primaryRole}</p>
                <p className="truncate text-xs">{user?.email}</p>
              </div>
            </div>
            <SidebarContent navItems={navItems} onNavigate={() => setMobileOpen(false)} compactBrand />
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/95 px-3 py-2.5 shadow-sm backdrop-blur sm:px-4 sm:py-3 lg:px-6">
          <div className="flex items-center justify-between gap-3">
            <div className="flex min-w-0 flex-1 items-center gap-2 sm:gap-3">
              <Button variant="ghost" size="icon" className="lg:hidden" aria-label="Menu" onClick={() => setMobileOpen(true)}>
                <Menu className="h-5 w-5" />
              </Button>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-zinc-950">{user?.fullName}</p>
                <p className="truncate text-xs text-zinc-500">
                  {primaryRole}<span className="hidden sm:inline"> · {user?.email}</span>
                </p>
              </div>
            </div>
            <Button variant="secondary" size="sm" aria-label="Đăng xuất" onClick={signOut}>
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Đăng xuất</span>
            </Button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-7xl px-3 py-4 sm:px-4 sm:py-6 lg:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function SidebarContent({
  navItems,
  onNavigate,
  compactBrand = false,
}: {
  navItems: NavItem[];
  onNavigate: () => void;
  compactBrand?: boolean;
}) {
  return (
    <>
      {!compactBrand && (
        <div className="flex h-16 items-center border-b border-white/10 px-5">
          <Brand />
        </div>
      )}
      <nav className="space-y-2 px-3 py-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "flex h-12 touch-manipulation items-center gap-3 rounded-2xl px-4 text-sm font-semibold transition hover:bg-white/10 hover:text-white lg:h-11",
                isActive ? "bg-teal-500 text-white" : "text-zinc-300 hover:bg-white/10 hover:text-white",
              )
            }
          >
            <item.icon className="h-5 w-5" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </>
  );
}

function Brand() {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-500 text-white sm:h-9 sm:w-9">
        <Building2 className="h-5 w-5" />
      </div>
      <div className="min-w-0">
        <p className="truncate text-sm font-semibold text-white">Rental Management</p>
        <p className="truncate text-xs text-zinc-400">Nhà trọ Việt Nam</p>
      </div>
    </div>
  );
}

function getNavItems(roles: string[]) {
  if (roles.includes("ADMIN")) {
    return adminNavItems;
  }
  if (roles.includes("TENANT")) {
    return tenantNavItems;
  }
  return landlordNavItems;
}

function getPrimaryRole(roles: string[]) {
  if (roles.includes("ADMIN")) {
    return "Admin";
  }
  if (roles.includes("TENANT")) {
    return "Khách thuê";
  }
  return "Chủ trọ";
}
