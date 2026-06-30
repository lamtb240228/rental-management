import { useMutation, useQuery } from "@tanstack/react-query";
import { Building2, DoorOpen, LockKeyhole, LockOpen, ReceiptText, ShieldCheck, UserCog, Users, Wrench } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { PageHeader } from "../../components/ui/page-header";
import { Table, Td, Th } from "../../components/ui/table";
import { queryClient } from "../../lib/query-client/queryClient";
import { useAuth } from "../auth/AuthProvider";
import { getAdminSummary, listAdminUsers, updateAdminUserStatus } from "./adminApi";

const stats = [
  { key: "userCount", label: "Tài khoản", icon: Users, className: "text-teal-700" },
  { key: "landlordCount", label: "Chủ trọ", icon: UserCog, className: "text-indigo-700" },
  { key: "tenantAccountCount", label: "Khách thuê", icon: ShieldCheck, className: "text-emerald-700" },
  { key: "propertyCount", label: "Khu trọ", icon: Building2, className: "text-sky-700" },
  { key: "roomCount", label: "Phòng", icon: DoorOpen, className: "text-cyan-700" },
  { key: "invoiceCount", label: "Hóa đơn", icon: ReceiptText, className: "text-violet-700" },
  { key: "pendingMaintenanceCount", label: "Sửa chữa chờ xử lý", icon: Wrench, className: "text-amber-700" },
] as const;

export function AdminDashboardPage() {
  const { user: currentUser } = useAuth();
  const summaryQuery = useQuery({ queryKey: ["admin", "summary"], queryFn: getAdminSummary });
  const usersQuery = useQuery({ queryKey: ["admin", "users"], queryFn: listAdminUsers });
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: "ACTIVE" | "LOCKED" }) => updateAdminUserStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  function statusAction(user: NonNullable<typeof usersQuery.data>[number]) {
    const nextStatus = user.status === "ACTIVE" ? "LOCKED" : "ACTIVE";
    return (
      <Button
        variant="secondary"
        size="sm"
        disabled={statusMutation.isPending || user.id === currentUser?.id}
        onClick={() => statusMutation.mutate({ id: user.id, status: nextStatus })}
      >
        {nextStatus === "LOCKED" ? <LockKeyhole className="h-4 w-4" /> : <LockOpen className="h-4 w-4" />}
        {nextStatus === "LOCKED" ? "Khóa" : "Mở khóa"}
      </Button>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Quản trị hệ thống"
        title="Tổng quan dữ liệu"
        description="Theo dõi nhanh tài khoản, khu trọ, phòng và hoạt động trong hệ thống."
      />

      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        {stats.map((item) => (
          <Card key={item.key} className="last:col-span-2">
            <CardContent className="flex min-h-24 items-center justify-between gap-2 p-4 sm:min-h-28 sm:gap-4 sm:p-5">
              <div className="min-w-0">
                <p className="text-xs leading-5 text-zinc-500 sm:text-sm">{item.label}</p>
                <p className="mt-1 text-2xl font-semibold text-zinc-950 sm:mt-2 sm:text-3xl">
                  {summaryQuery.data ? summaryQuery.data[item.key] : 0}
                </p>
              </div>
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-50">
                <item.icon className={`h-5 w-5 sm:h-6 sm:w-6 ${item.className}`} />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tài khoản trong hệ thống</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="hidden overflow-x-auto sm:block">
            <Table>
              <thead>
                <tr>
                  <Th>Người dùng</Th>
                  <Th>Liên hệ</Th>
                  <Th>Vai trò</Th>
                  <Th>Trạng thái</Th>
                  <Th>Đăng nhập gần nhất</Th>
                  <Th>Thao tác</Th>
                </tr>
              </thead>
              <tbody>
                {usersQuery.data?.map((user) => (
                  <tr key={user.id}>
                    <Td>
                      <p className="font-medium text-zinc-950">{user.fullName}</p>
                      <p className="text-xs text-zinc-500">{user.email}</p>
                    </Td>
                    <Td>{user.phone ?? ""}</Td>
                    <Td>
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map((role) => (
                          <Badge key={role}>{role}</Badge>
                        ))}
                      </div>
                    </Td>
                    <Td>
                      <Badge>{user.status}</Badge>
                    </Td>
                    <Td>{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString("vi-VN") : "Chưa có"}</Td>
                    <Td>{statusAction(user)}</Td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </div>
          <div className="space-y-3 sm:hidden">
            {usersQuery.data?.map((user) => (
              <div key={user.id} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-zinc-950">{user.fullName}</p>
                    <p className="mt-1 break-all text-xs text-zinc-500">{user.email}</p>
                  </div>
                  <Badge>{user.status}</Badge>
                </div>
                <div className="mt-4 grid grid-cols-2 gap-2">
                  <div className="min-w-0 rounded-xl bg-slate-50 p-3">
                    <p className="text-xs font-medium text-zinc-500">Vai trò</p>
                    <div className="mt-2 flex flex-wrap gap-1">
                      {user.roles.map((role) => <Badge key={role}>{role}</Badge>)}
                    </div>
                  </div>
                  <div className="min-w-0 rounded-xl bg-slate-50 p-3">
                    <p className="text-xs font-medium text-zinc-500">Điện thoại</p>
                    <p className="mt-1 break-words text-sm text-zinc-950">{user.phone ?? "—"}</p>
                  </div>
                  <div className="col-span-2 rounded-xl bg-slate-50 p-3">
                    <p className="text-xs font-medium text-zinc-500">Đăng nhập gần nhất</p>
                    <p className="mt-1 text-sm text-zinc-950">
                      {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString("vi-VN") : "Chưa có"}
                    </p>
                  </div>
                  <div className="col-span-2">{statusAction(user)}</div>
                </div>
              </div>
            ))}
            {usersQuery.data?.length === 0 && (
              <div className="rounded-2xl border border-dashed border-zinc-200 p-6 text-center text-sm text-zinc-500">
                Chưa có tài khoản.
              </div>
            )}
          </div>
        </CardContent>
      </Card>
      {statusMutation.isError && <p className="text-sm text-red-600">Không thể thay đổi trạng thái tài khoản.</p>}
    </div>
  );
}
