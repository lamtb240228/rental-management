import { useQuery } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { PageHeader } from "../components/ui/page-header";
import { Table, Td, Th } from "../components/ui/table";
import { apiClient } from "../lib/api/client";
import type { ApiResponse } from "../lib/api/types";

type Props = {
  title: string;
  endpoint: string;
};

const preferredColumns: Record<string, string[]> = {
  "/tenants": ["fullName", "phone", "email", "identityNumber", "status"],
  "/contracts": ["contractCode", "roomNumber", "startDate", "endDate", "monthlyRent", "status"],
  "/invoices": ["invoiceNumber", "billingMonth", "billingYear", "totalAmount", "paidAmount", "status"],
  "/maintenance-requests": ["title", "roomNumber", "tenantName", "priority", "status", "submittedAt"],
  "/admin/users": ["fullName", "email", "phone", "roles", "status", "lastLoginAt"],
};

const labels: Record<string, string> = {
  billingMonth: "Tháng",
  billingYear: "Năm",
  contractCode: "Mã hợp đồng",
  email: "Email",
  endDate: "Ngày kết thúc",
  fullName: "Họ tên",
  identityNumber: "CCCD/CMND",
  invoiceNumber: "Mã hóa đơn",
  lastLoginAt: "Đăng nhập gần nhất",
  monthlyRent: "Giá thuê",
  paidAmount: "Đã trả",
  phone: "Số điện thoại",
  priority: "Mức độ",
  roles: "Vai trò",
  roomNumber: "Phòng",
  startDate: "Ngày bắt đầu",
  status: "Trạng thái",
  submittedAt: "Ngày gửi",
  tenantName: "Người thuê",
  title: "Tiêu đề",
  totalAmount: "Tổng tiền",
};

const endpointDescriptions: Record<string, string> = {
  "/tenants": "Danh sách người thuê trong hệ thống, xem trạng thái và chi tiết liên lạc.",
  "/contracts": "Quản lý hợp đồng thuê đang hoạt động và lịch sử hợp đồng.",
  "/invoices": "Theo dõi hóa đơn, số tiền cần thanh toán và trạng thái thanh toán.",
  "/maintenance-requests": "Xem các yêu cầu sửa chữa và tình trạng xử lý.",
  "/admin/users": "Quản lý tài khoản hệ thống và quyền truy cập của người dùng.",
};

export function SimpleListPage({ title, endpoint }: Props) {
  const query = useQuery({
    queryKey: [endpoint],
    queryFn: async () => {
      const response = await apiClient.get<ApiResponse<Record<string, unknown>[]>>(endpoint);
      return response.data.data;
    },
  });

  const rows = query.data ?? [];
  const fallbackColumns = Array.from(new Set(rows.flatMap((row) => Object.keys(row)))).slice(0, 6);
  const columns = preferredColumns[endpoint] ?? fallbackColumns;
  const description = endpointDescriptions[endpoint];
  const errorMessage = query.error ? String(query.error) : undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Danh sách"
        title={title}
        description={description}
        action={
          <Button variant="secondary" onClick={() => query.refetch()}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
        </CardHeader>
        <CardContent>
          {query.isLoading ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 p-6 text-center text-sm text-zinc-500 sm:rounded-3xl sm:p-8">
              Đang tải dữ liệu...
            </div>
          ) : errorMessage ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 sm:rounded-3xl sm:p-6">
              Lỗi khi tải dữ liệu: {errorMessage}
            </div>
          ) : rows.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 p-6 text-center text-sm text-zinc-500 sm:rounded-3xl sm:p-8">
              Chưa có dữ liệu để hiển thị. Hãy tạo mục mới hoặc kiểm tra lại nguồn dữ liệu.
            </div>
          ) : (
            <>
              <div className="hidden overflow-x-auto sm:block">
                <Table>
                  <thead>
                    <tr>
                      {(columns.length ? columns : ["id"]).map((column) => (
                        <Th key={column}>{labels[column] ?? column}</Th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, index) => (
                      <tr key={String(row.id ?? index)}>
                        {(columns.length ? columns : ["id"]).map((column) => (
                          <Td key={column}>{formatCell(row[column], column)}</Td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
              <div className="space-y-3 sm:hidden">
                {rows.map((row, index) => (
                  <div key={String(row.id ?? index)} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-zinc-950">{String(row[columns[0] ?? "id"] ?? row.id ?? `Mục ${index + 1}`)}</p>
                        {labels[columns[1]] && row[columns[1]] != null && (
                          <p className="mt-1 text-sm text-zinc-500 truncate">{labels[columns[1]]}: {String(row[columns[1]])}</p>
                        )}
                      </div>
                      {typeof row.status === "string" && <Badge>{row.status}</Badge>}
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-2">
                      {columns.filter((column) => column !== columns[0] && column !== "status").slice(0, 4).map((column) => (
                        <div key={column} className="min-w-0 rounded-xl bg-zinc-50 p-3">
                          <p className="text-xs font-medium text-zinc-500">{labels[column] ?? column}</p>
                          <div className="mt-1 break-words text-sm text-zinc-900">{formatCell(row[column], column)}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function formatCell(value: unknown, column: string) {
  if (value == null) {
    return "";
  }
  if (Array.isArray(value)) {
    return (
      <div className="flex flex-wrap gap-1">
        {value.map((item) => (
          <Badge key={String(item)}>{String(item)}</Badge>
        ))}
      </div>
    );
  }
  if (column.toLowerCase().includes("amount") || column === "monthlyRent") {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0,
    }).format(Number(value));
  }
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return new Date(value).toLocaleString("vi-VN");
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  if (column === "status" || column === "priority") {
    return <Badge>{String(value)}</Badge>;
  }
  return String(value);
}
