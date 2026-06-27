import { Route, Routes } from "react-router-dom";
import { AppLayout } from "../layouts/AppLayout";
import { AdminDashboardPage } from "../../features/admin/AdminDashboardPage";
import { useAuth } from "../../features/auth/AuthProvider";
import { LoginPage } from "../../features/auth/LoginPage";
import { ProtectedRoute } from "../../features/auth/ProtectedRoute";
import { RegisterPage } from "../../features/auth/RegisterPage";
import { DashboardPage } from "../../features/dashboard/DashboardPage";
import { PropertiesPage } from "../../features/properties/PropertiesPage";
import { TenantPortalPage } from "../../features/tenant/TenantPortalPage";
import { ContractsPage } from "../../features/contracts/ContractsPage";
import { InvoicesPage } from "../../features/invoices/InvoicesPage";
import { MaintenancePage } from "../../features/maintenance/MaintenancePage";
import { TenantsPage } from "../../features/tenants/TenantsPage";
import { SimpleListPage } from "../../pages/SimpleListPage";

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<RoleHomePage />} />
        <Route path="admin/users" element={<SimpleListPage title="Tài khoản" endpoint="/admin/users" />} />
        <Route path="properties" element={<PropertiesPage />} />
        <Route path="tenants" element={<TenantsPage />} />
        <Route path="contracts" element={<ContractsPage />} />
        <Route path="invoices" element={<InvoicesPage />} />
        <Route path="maintenance" element={<MaintenancePage />} />
      </Route>
    </Routes>
  );
}

function RoleHomePage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];

  if (roles.includes("ADMIN")) {
    return <AdminDashboardPage />;
  }
  if (roles.includes("TENANT")) {
    return <TenantPortalPage />;
  }
  return <DashboardPage />;
}
