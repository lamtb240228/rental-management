import {
  Building2,
  FileText,
  Home,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  ShieldCheck,
  UserCog,
  Users,
  Wrench,
  X,
} from "lucide-react";
import { useState } from "react";
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

  return (
    <div className="min-h-screen bg-slate-50 text-zinc-900">
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-64 border-r border-slate-800 bg-slate-900/95 text-white shadow-2xl lg:block">
        <div className="px-5 py-5">
          <Brand />
          <p className="mt-3 text-sm text-slate-300">Quản lý thuê trọ, hợp đồng, hóa đơn và bảo trì dễ dàng.</p>
        </div>
        <div className="border-t border-white/10" />
        <SidebarContent navItems={navItems} onNavigate={() => setMobileOpen(false)} compactBrand />
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-30 lg:hidden">
          <button className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" aria-label="Đóng menu" onClick={() => setMobileOpen(false)} />
          <aside className="relative h-full w-72 border-r border-white/10 bg-slate-900/95 text-white shadow-2xl">
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
        <header className="sticky top-0 z-10 border-b border-slate-200/80 bg-white/95 px-4 py-3 shadow-sm backdrop-blur lg:px-6">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <Button variant="ghost" size="icon" className="lg:hidden" aria-label="Menu" onClick={() => setMobileOpen(true)}>
                <Menu className="h-5 w-5" />
              </Button>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-zinc-950">{user?.fullName}</p>
                <p className="truncate text-xs text-zinc-500">
                  {primaryRole} · {user?.email}
                </p>
              </div>
            </div>
            <Button variant="secondary" size="sm" onClick={signOut}>
              <LogOut className="h-4 w-4" />
              Đăng xuất
            </Button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-7xl px-4 py-6 lg:px-6">
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
      <nav className="space-y-2 px-3 py-4">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "flex h-11 items-center gap-3 rounded-2xl px-4 text-sm font-semibold transition hover:bg-white/10 hover:text-white",
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
      <div className="flex h-9 w-9 items-center justify-center rounded-md bg-teal-500 text-white">
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
