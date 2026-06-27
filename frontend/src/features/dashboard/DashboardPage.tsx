import { useQuery } from "@tanstack/react-query";
import { Building2, DoorOpen, FileText, Home, TrendingUp, Wrench } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { getDashboardSummary } from "./dashboardApi";

const statMeta = [
  { key: "propertyCount", label: "Khu trọ", icon: Building2, color: "text-teal-700" },
  { key: "availableRoomCount", label: "Phòng trống", icon: DoorOpen, color: "text-emerald-700" },
  { key: "occupiedRoomCount", label: "Đang thuê", icon: Home, color: "text-sky-700" },
  { key: "invoiceCount", label: "Hóa đơn", icon: FileText, color: "text-violet-700" },
  { key: "pendingMaintenanceCount", label: "Chờ sửa", icon: Wrench, color: "text-amber-700" },
] as const;

export function DashboardPage() {
  const summaryQuery = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: getDashboardSummary,
  });

  const available = summaryQuery.data?.availableRoomCount ?? 0;
  const occupied = summaryQuery.data?.occupiedRoomCount ?? 0;
  const totalRooms = available + occupied;
  const occupancyRate = totalRooms === 0 ? 0 : Math.round((occupied / totalRooms) * 100);

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-teal-100 bg-gradient-to-r from-teal-600 via-emerald-500 to-cyan-500 p-4 text-white shadow-lg sm:rounded-3xl sm:p-6">
        <p className="text-sm font-medium text-teal-50">Chủ trọ</p>
        <h1 className="mt-1 text-xl font-semibold sm:text-2xl">Dashboard vận hành</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-teal-50/90">
          Theo dõi tình trạng phòng trọ, công việc cần xử lý và hiệu suất sử dụng một cách rõ ràng.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-5">
        {statMeta.map((item) => (
          <Card key={item.key} className="overflow-hidden last:col-span-2 xl:last:col-span-1">
            <CardContent className="flex min-h-24 items-center justify-between gap-2 p-4 sm:min-h-28 sm:gap-4 sm:p-5">
              <div className="min-w-0">
                <p className="text-xs leading-5 text-zinc-500 sm:text-sm">{item.label}</p>
                <p className="mt-1 text-2xl font-semibold text-zinc-950 sm:mt-2 sm:text-3xl">
                  {summaryQuery.data ? summaryQuery.data[item.key] : 0}
                </p>
              </div>
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-zinc-50 sm:h-12 sm:w-12 sm:rounded-2xl">
                <item.icon className={`h-5 w-5 sm:h-6 sm:w-6 ${item.color}`} />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <CardHeader>
            <CardTitle>Tỷ lệ lấp đầy</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-end gap-3">
              <p className="text-4xl font-semibold text-zinc-950 sm:text-5xl">{occupancyRate}%</p>
              <TrendingUp className="mb-2 h-6 w-6 text-teal-700" />
            </div>
            <div className="mt-5 h-3 overflow-hidden rounded-full bg-zinc-100">
              <div className="h-full rounded-full bg-gradient-to-r from-teal-500 to-emerald-500" style={{ width: `${occupancyRate}%` }} />
            </div>
            <p className="mt-3 text-sm text-zinc-500">
              {occupied} phòng đang thuê, {available} phòng trống
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Việc cần chú ý</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2">
            <WorkItem label="Yêu cầu sửa chữa chờ xử lý" value={summaryQuery.data?.pendingMaintenanceCount ?? 0} />
            <WorkItem label="Hóa đơn trong hệ thống" value={summaryQuery.data?.invoiceCount ?? 0} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function WorkItem({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4">
      <p className="text-sm text-zinc-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-zinc-950">{value}</p>
    </div>
  );
}
