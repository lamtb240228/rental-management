import { useQuery } from "@tanstack/react-query";
import { Building2, DoorOpen, ReceiptText, ShieldCheck, UserCog, Users, Wrench } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Table, Td, Th } from "../../components/ui/table";
import { getAdminSummary, listAdminUsers } from "./adminApi";

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
  const summaryQuery = useQuery({ queryKey: ["admin", "summary"], queryFn: getAdminSummary });
  const usersQuery = useQuery({ queryKey: ["admin", "users"], queryFn: listAdminUsers });

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-teal-700">Quản trị hệ thống</p>
        <h1 className="mt-1 text-2xl font-semibold text-zinc-950">Tổng quan dữ liệu</h1>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((item) => (
          <Card key={item.key}>
            <CardContent className="flex min-h-28 items-center justify-between gap-4">
              <div>
                <p className="text-sm text-zinc-500">{item.label}</p>
                <p className="mt-2 text-3xl font-semibold text-zinc-950">
                  {summaryQuery.data ? summaryQuery.data[item.key] : 0}
                </p>
              </div>
              <item.icon className={`h-7 w-7 ${item.className}`} />
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tài khoản trong hệ thống</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <thead>
                <tr>
                  <Th>Người dùng</Th>
                  <Th>Liên hệ</Th>
                  <Th>Vai trò</Th>
                  <Th>Trạng thái</Th>
                  <Th>Đăng nhập gần nhất</Th>
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
                  </tr>
                ))}
              </tbody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
