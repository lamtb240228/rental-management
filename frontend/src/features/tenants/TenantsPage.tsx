import { useMutation, useQuery } from "@tanstack/react-query";
import { History, Pencil, RefreshCw, UserRoundPlus, X } from "lucide-react";
import { useState } from "react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { PageHeader } from "../../components/ui/page-header";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { Textarea } from "../../components/ui/textarea";
import type { TenantItem } from "../../lib/api/types";
import { queryClient } from "../../lib/query-client/queryClient";
import { formatCurrency } from "../../lib/utils";
import { createTenant, listTenantContracts, listTenants, updateTenant, type TenantPayload } from "./tenantApi";

const emptyTenant = (): TenantPayload => ({
  fullName: "",
  dateOfBirth: "",
  phone: "",
  email: "",
  identityNumber: "",
  permanentAddress: "",
  status: "ACTIVE",
});

function maskIdentityNumber(identityNumber?: string) {
  if (!identityNumber) return "—";
  return `••••••${identityNumber.slice(-4)}`;
}

export function TenantsPage() {
  const [form, setForm] = useState<TenantPayload>(emptyTenant);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [historyTenant, setHistoryTenant] = useState<TenantItem | null>(null);
  const tenantsQuery = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const historyQuery = useQuery({
    queryKey: ["tenants", historyTenant?.id, "contracts"],
    queryFn: () => listTenantContracts(historyTenant!.id),
    enabled: historyTenant != null,
  });

  const saveMutation = useMutation({
    mutationFn: () => editingId ? updateTenant(editingId, form) : createTenant(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tenants"] });
      setForm(emptyTenant());
      setEditingId(null);
    },
  });

  function beginEdit(tenant: TenantItem) {
    setEditingId(tenant.id);
    setForm({
      fullName: tenant.fullName,
      dateOfBirth: tenant.dateOfBirth ?? "",
      phone: tenant.phone ?? "",
      email: tenant.email ?? "",
      identityNumber: tenant.identityNumber ?? "",
      permanentAddress: tenant.permanentAddress ?? "",
      status: tenant.status,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyTenant());
  }

  function rowActions(tenant: TenantItem) {
    return (
      <div className="flex gap-1">
        <Button variant="ghost" size="icon" title="Chỉnh sửa" onClick={() => beginEdit(tenant)}><Pencil className="h-4 w-4" /></Button>
        <Button variant="ghost" size="icon" title="Lịch sử thuê" onClick={() => setHistoryTenant(tenant)}><History className="h-4 w-4" /></Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Quản lý thuê phòng"
        title="Người thuê"
        description="Lưu hồ sơ, cập nhật trạng thái và xem toàn bộ lịch sử hợp đồng của từng người thuê."
        action={<Button variant="secondary" onClick={() => tenantsQuery.refetch()}><RefreshCw className="h-4 w-4" />Tải lại</Button>}
      />

      <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
        <Card className="order-2 xl:order-1">
          <CardHeader><CardTitle>{editingId ? "Cập nhật người thuê" : "Thêm người thuê"}</CardTitle></CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); saveMutation.mutate(); }}>
              <Field label="Họ tên" id="tenant-name"><Input id="tenant-name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></Field>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
                <Field label="Ngày sinh" id="tenant-birth"><Input id="tenant-birth" type="date" value={form.dateOfBirth ?? ""} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} /></Field>
                <Field label="Số điện thoại" id="tenant-phone"><Input id="tenant-phone" value={form.phone ?? ""} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
              </div>
              <Field label="Email" id="tenant-email"><Input id="tenant-email" type="email" value={form.email ?? ""} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
              <Field label="CCCD/CMND" id="tenant-identity"><Input id="tenant-identity" value={form.identityNumber ?? ""} onChange={(e) => setForm({ ...form, identityNumber: e.target.value })} /></Field>
              <Field label="Trạng thái" id="tenant-status"><Select id="tenant-status" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as TenantPayload["status"] })}><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option></Select></Field>
              <Field label="Địa chỉ thường trú" id="tenant-address"><Textarea id="tenant-address" value={form.permanentAddress ?? ""} onChange={(e) => setForm({ ...form, permanentAddress: e.target.value })} /></Field>
              <div className="flex gap-2">
                <Button className="flex-1" disabled={saveMutation.isPending}><UserRoundPlus className="h-4 w-4" />{saveMutation.isPending ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Lưu người thuê"}</Button>
                {editingId && <Button type="button" variant="secondary" size="icon" title="Hủy chỉnh sửa" onClick={cancelEdit}><X className="h-4 w-4" /></Button>}
              </div>
              {saveMutation.isError && <p className="text-sm text-red-600">Không thể lưu hồ sơ. Email hoặc CCCD có thể đã tồn tại.</p>}
            </form>
          </CardContent>
        </Card>

        <Card className="order-1 xl:order-2">
          <CardHeader><CardTitle>Danh sách người thuê</CardTitle></CardHeader>
          <CardContent>
            <div className="hidden overflow-x-auto sm:block">
              <Table><thead><tr><Th>Họ tên</Th><Th>Liên hệ</Th><Th>CCCD</Th><Th>Trạng thái</Th><Th>Thao tác</Th></tr></thead>
                <tbody>{(tenantsQuery.data ?? []).map((tenant) => (
                  <tr key={tenant.id}><Td className="font-medium text-zinc-950">{tenant.fullName}</Td><Td><p>{tenant.phone ?? "—"}</p><p className="text-xs text-zinc-500">{tenant.email ?? "—"}</p></Td><Td>{maskIdentityNumber(tenant.identityNumber)}</Td><Td><Badge>{tenant.status}</Badge></Td><Td>{rowActions(tenant)}</Td></tr>
                ))}</tbody>
              </Table>
            </div>
            <div className="space-y-3 sm:hidden">
              {(tenantsQuery.data ?? []).map((tenant) => (
                <div key={tenant.id} className="rounded-lg border border-zinc-200 bg-white p-4">
                  <div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="truncate text-sm font-semibold">{tenant.fullName}</p><p className="mt-1 break-all text-xs text-zinc-500">{tenant.email ?? "Không có email"}</p></div><Badge>{tenant.status}</Badge></div>
                  <div className="mt-4 grid grid-cols-2 gap-2 text-sm"><div className="rounded-lg bg-slate-50 p-3"><span className="text-xs text-zinc-500">Điện thoại</span><p>{tenant.phone ?? "—"}</p></div><div className="rounded-lg bg-slate-50 p-3"><span className="text-xs text-zinc-500">CCCD</span><p className="break-all">{maskIdentityNumber(tenant.identityNumber)}</p></div></div>
                  <div className="mt-3">{rowActions(tenant)}</div>
                </div>
              ))}
            </div>
            {!tenantsQuery.data?.length && <p className="py-8 text-center text-sm text-zinc-500">Chưa có người thuê.</p>}
          </CardContent>
        </Card>
      </div>

      {historyTenant && (
        <Card>
          <CardHeader className="flex-row items-center justify-between"><div><CardTitle>Lịch sử thuê của {historyTenant.fullName}</CardTitle><p className="mt-1 text-sm text-zinc-500">Các hợp đồng hiện tại và đã kết thúc.</p></div><Button variant="ghost" size="icon" title="Đóng" onClick={() => setHistoryTenant(null)}><X className="h-4 w-4" /></Button></CardHeader>
          <CardContent>
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {(historyQuery.data ?? []).map((contract) => (
                <div key={contract.id} className="rounded-lg border border-zinc-200 p-4"><div className="flex justify-between gap-2"><strong>{contract.contractCode}</strong><Badge>{contract.status}</Badge></div><p className="mt-2 text-sm text-zinc-500">Phòng {contract.roomNumber}</p><dl className="mt-4 grid grid-cols-2 gap-2 text-sm"><div><dt className="text-xs text-zinc-500">Thời hạn</dt><dd>{contract.startDate}<br />{contract.endDate ?? "Chưa xác định"}</dd></div><div><dt className="text-xs text-zinc-500">Giá thuê</dt><dd>{formatCurrency(Number(contract.monthlyRent))}</dd></div></dl></div>
              ))}
            </div>
            {historyQuery.data?.length === 0 && <p className="py-6 text-sm text-zinc-500">Người thuê chưa có hợp đồng.</p>}
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function Field({ label, id, children }: { label: string; id: string; children: React.ReactNode }) {
  return <div className="space-y-2"><Label htmlFor={id}>{label}</Label>{children}</div>;
}
