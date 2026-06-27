import { useMutation, useQuery } from "@tanstack/react-query";
import { RefreshCw, UserRoundPlus } from "lucide-react";
import { useState } from "react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { Textarea } from "../../components/ui/textarea";
import { queryClient } from "../../lib/query-client/queryClient";
import { createTenant, listTenants, type TenantPayload } from "./tenantApi";

export function TenantsPage() {
  const [form, setForm] = useState<TenantPayload>({
    fullName: "",
    phone: "",
    email: "",
    identityNumber: "",
    permanentAddress: "",
    status: "ACTIVE",
  });

  const tenantsQuery = useQuery({ queryKey: ["tenants"], queryFn: listTenants });

  const createTenantMutation = useMutation({
    mutationFn: createTenant,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
      setForm({
        fullName: "",
        phone: "",
        email: "",
        identityNumber: "",
        permanentAddress: "",
        status: "ACTIVE",
      });
    },
  });

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    createTenantMutation.mutate(form);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-teal-700">Quản lý thuê phòng</p>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-950">Quản lý người thuê</h1>
        </div>
        <Button variant="secondary" onClick={() => tenantsQuery.refetch()}>
          <RefreshCw className="h-4 w-4" />
          Tải lại
        </Button>
      </div>

      <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Thêm người thuê</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="fullName">Họ tên</Label>
                <Input
                  id="fullName"
                  value={form.fullName}
                  onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại</Label>
                <Input
                  id="phone"
                  value={form.phone ?? ""}
                  onChange={(event) => setForm({ ...form, phone: event.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={form.email ?? ""}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="identityNumber">CCCD/CMND</Label>
                <Input
                  id="identityNumber"
                  value={form.identityNumber ?? ""}
                  onChange={(event) => setForm({ ...form, identityNumber: event.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="status">Trạng thái</Label>
                <Select
                  id="status"
                  value={form.status}
                  onChange={(event) => setForm({ ...form, status: event.target.value as TenantPayload["status"] })}
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="permanentAddress">Địa chỉ thường trú</Label>
                <Textarea
                  id="permanentAddress"
                  value={form.permanentAddress ?? ""}
                  onChange={(event) => setForm({ ...form, permanentAddress: event.target.value })}
                />
              </div>
              <Button className="w-full" disabled={createTenantMutation.isPending}>
                <UserRoundPlus className="h-4 w-4" />
                {createTenantMutation.isPending ? "Đang lưu..." : "Lưu người thuê"}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Danh sách người thuê</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="hidden sm:block overflow-x-auto">
              <Table>
                <thead>
                  <tr>
                    <Th>Họ tên</Th>
                    <Th>Điện thoại</Th>
                    <Th>Email</Th>
                    <Th>CCCD</Th>
                    <Th>Trạng thái</Th>
                  </tr>
                </thead>
                <tbody>
                  {(tenantsQuery.data ?? []).map((tenant) => (
                    <tr key={tenant.id}>
                      <Td className="font-medium text-zinc-950">{tenant.fullName}</Td>
                      <Td>{tenant.phone ?? "—"}</Td>
                      <Td>{tenant.email ?? "—"}</Td>
                      <Td>{tenant.identityNumber ?? "—"}</Td>
                      <Td>
                        <Badge>{tenant.status}</Badge>
                      </Td>
                    </tr>
                  ))}
                  {!tenantsQuery.data?.length && (
                    <tr>
                      <Td colSpan={5} className="text-zinc-500">
                        Chưa có người thuê
                      </Td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            <div className="space-y-4 sm:hidden">
              {(tenantsQuery.data ?? []).map((tenant) => (
                <div key={tenant.id} className="rounded-3xl border border-zinc-200 bg-white p-4 shadow-sm">
                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-zinc-950 truncate">{tenant.fullName}</p>
                      <p className="mt-1 text-xs text-zinc-500 truncate">{tenant.email ?? "Không có email"}</p>
                    </div>
                    <Badge>{tenant.status}</Badge>
                  </div>
                  <div className="mt-4 grid gap-3">
                    <div className="rounded-2xl bg-slate-50 p-3">
                      <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">Điện thoại</p>
                      <p className="mt-1 text-sm text-zinc-950">{tenant.phone ?? "—"}</p>
                    </div>
                    <div className="rounded-2xl bg-slate-50 p-3">
                      <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">CCCD</p>
                      <p className="mt-1 text-sm text-zinc-950">{tenant.identityNumber ?? "—"}</p>
                    </div>
                  </div>
                </div>
              ))}
              {!tenantsQuery.data?.length && (
                <div className="rounded-3xl border border-zinc-200 bg-white p-6 text-center text-sm text-zinc-500">
                  Chưa có người thuê
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
