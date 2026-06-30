import { Route, Routes } from "react-router-dom";
import { AppLayout } from "../layouts/AppLayout";
import { AdminDashboardPage } from "../../features/admin/AdminDashboardPage";
import { useAuth } from "../../features/auth/AuthProvider";
import { LoginPage } from "../../features/auth/LoginPage";
import { ProtectedRoute } from "../../features/auth/ProtectedRoute";
import { RoleRoute } from "../../features/auth/RoleRoute";
import { RegisterPage } from "../../features/auth/RegisterPage";
import { DashboardPage } from "../../features/dashboard/DashboardPage";
import { PropertiesPage } from "../../features/properties/PropertiesPage";
import { TenantPortalPage } from "../../features/tenant/TenantPortalPage";
import { ContractsPage } from "../../features/contracts/ContractsPage";
import { InvoicesPage } from "../../features/invoices/InvoicesPage";
import { MaintenancePage } from "../../features/maintenance/MaintenancePage";
import { TenantsPage } from "../../features/tenants/TenantsPage";
import { UtilitiesPage } from "../../features/utilities/UtilitiesPage";

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
        <Route path="admin/users" element={<RoleRoute roles={["ADMIN"]}><AdminDashboardPage /></RoleRoute>} />
        <Route path="properties" element={<RoleRoute roles={["LANDLORD"]}><PropertiesPage /></RoleRoute>} />
        <Route path="tenants" element={<RoleRoute roles={["LANDLORD"]}><TenantsPage /></RoleRoute>} />
        <Route path="contracts" element={<RoleRoute roles={["LANDLORD"]}><ContractsPage /></RoleRoute>} />
        <Route path="utilities" element={<RoleRoute roles={["LANDLORD"]}><UtilitiesPage /></RoleRoute>} />
        <Route path="invoices" element={<RoleRoute roles={["LANDLORD"]}><InvoicesPage /></RoleRoute>} />
        <Route path="maintenance" element={<RoleRoute roles={["LANDLORD"]}><MaintenancePage /></RoleRoute>} />
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
