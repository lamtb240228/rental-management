import { useMutation, useQuery } from "@tanstack/react-query";
import { AlertCircle, ClipboardList, Home, ReceiptText, Send, Wrench, Zap } from "lucide-react";
import { useState } from "react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { PageHeader } from "../../components/ui/page-header";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { Textarea } from "../../components/ui/textarea";
import { formatCurrency } from "../../lib/utils";
import { queryClient } from "../../lib/query-client/queryClient";
import { createTenantMaintenanceRequest, getTenantPortalSummary } from "./tenantPortalApi";

export function TenantPortalPage() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<"LOW" | "MEDIUM" | "HIGH" | "URGENT">("MEDIUM");
  const portalQuery = useQuery({ queryKey: ["tenant-portal", "summary"], queryFn: getTenantPortalSummary });

  const maintenanceMutation = useMutation({
    mutationFn: createTenantMaintenanceRequest,
    onSuccess: () => {
      setTitle("");
      setDescription("");
      setPriority("MEDIUM");
      queryClient.invalidateQueries({ queryKey: ["tenant-portal", "summary"] });
    },
  });

  const data = portalQuery.data;
  const unpaidInvoices = data?.invoices.filter((invoice) => invoice.status !== "PAID" && invoice.status !== "CANCELLED") ?? [];
  const currentInvoice = unpaidInvoices[0] ?? data?.invoices[0];
  const latestReading = data?.utilityReadings[0];

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Cổng khách thuê"
        title={`Xin chào, ${data?.tenant.fullName ?? "khách thuê"}`}
        description="Xem hợp đồng, hóa đơn, điện nước và gửi yêu cầu sửa chữa một cách nhanh chóng."
      />
      {portalQuery.isLoading && (
        <div className="rounded-2xl border border-dashed border-zinc-200 p-6 text-center text-sm text-zinc-500 sm:rounded-3xl sm:p-8">
          Đang tải thông tin...
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <MetricCard
          icon={Home}
          label="Phòng đang thuê"
          value={data?.room?.roomNumber ?? "Chưa có"}
          detail={data?.room?.propertyName ?? ""}
          tone="text-sky-700"
        />
        <MetricCard
          icon={ClipboardList}
          label="Hợp đồng"
          value={data?.activeContract?.status ?? "Chưa có"}
          detail={data?.activeContract?.contractCode ?? ""}
          tone="text-indigo-700"
        />
        <MetricCard
          icon={ReceiptText}
          label="Cần thanh toán"
          value={formatCurrency(currentInvoice ? currentInvoice.totalAmount - currentInvoice.paidAmount : 0)}
          detail={currentInvoice ? `${currentInvoice.billingMonth}/${currentInvoice.billingYear}` : ""}
          tone="text-violet-700"
        />
        <MetricCard
          icon={Wrench}
          label="Yêu cầu sửa chữa"
          value={`${data?.maintenanceRequests.length ?? 0}`}
          detail="Tổng yêu cầu đã gửi"
          tone="text-amber-700"
        />
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
        <Card>
          <CardHeader>
            <CardTitle>Hóa đơn gần đây</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="hidden overflow-x-auto sm:block">
              <Table>
                <thead>
                  <tr>
                    <Th>Mã hóa đơn</Th>
                    <Th>Kỳ</Th>
                    <Th>Tổng tiền</Th>
                    <Th>Đã trả</Th>
                    <Th>Trạng thái</Th>
                  </tr>
                </thead>
                <tbody>
                  {data?.invoices.slice(0, 6).map((invoice) => (
                    <tr key={invoice.id}>
                      <Td className="font-medium text-zinc-950">{invoice.invoiceNumber}</Td>
                      <Td>{invoice.billingMonth}/{invoice.billingYear}</Td>
                      <Td>{formatCurrency(invoice.totalAmount)}</Td>
                      <Td>{formatCurrency(invoice.paidAmount)}</Td>
                      <Td>
                        <Badge>{invoice.status}</Badge>
                      </Td>
                    </tr>
                  ))}
                  {data?.invoices.length === 0 && (
                    <tr>
                      <Td colSpan={5} className="text-zinc-500">Chưa có hóa đơn</Td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            <div className="space-y-3 sm:hidden">
              {data?.invoices.slice(0, 6).map((invoice) => (
                <div key={invoice.id} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-zinc-950 truncate">{invoice.invoiceNumber}</p>
                      <p className="mt-1 text-sm text-zinc-500">{invoice.billingMonth}/{invoice.billingYear}</p>
                    </div>
                    <Badge>{invoice.status}</Badge>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2">
                    <div className="min-w-0 rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Tổng tiền</p>
                      <p className="mt-1 text-sm text-zinc-950">{formatCurrency(invoice.totalAmount)}</p>
                    </div>
                    <div className="min-w-0 rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Đã trả</p>
                      <p className="mt-1 text-sm text-zinc-950">{formatCurrency(invoice.paidAmount)}</p>
                    </div>
                  </div>
                </div>
              ))}
              {data?.invoices.length === 0 && (
                <div className="rounded-2xl border border-dashed border-zinc-200 bg-white p-6 text-center text-sm text-zinc-500">
                  Chưa có hóa đơn
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Gửi yêu cầu sửa chữa</CardTitle>
          </CardHeader>
          <CardContent>
            <form
              className="space-y-3"
              onSubmit={(event) => {
                event.preventDefault();
                maintenanceMutation.mutate({ title, description, priority });
              }}
            >
              <Input
                placeholder="Tiêu đề"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                required
              />
              <Select value={priority} onChange={(event) => setPriority(event.target.value as typeof priority)}>
                <option value="LOW">Thấp</option>
                <option value="MEDIUM">Trung bình</option>
                <option value="HIGH">Cao</option>
                <option value="URGENT">Khẩn cấp</option>
              </Select>
              <Textarea
                placeholder="Mô tả vấn đề"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                required
              />
              <Button className="w-full" disabled={maintenanceMutation.isPending || !data?.activeContract}>
                <Send className="h-4 w-4" />
                Gửi yêu cầu
              </Button>
              {!data?.activeContract && (
                <p className="flex items-center gap-2 text-sm text-amber-700">
                  <AlertCircle className="h-4 w-4" />
                  Tài khoản chưa có hợp đồng đang hiệu lực.
                </p>
              )}
            </form>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-5 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Chỉ số điện nước</CardTitle>
          </CardHeader>
          <CardContent>
            {latestReading ? (
              <div className="grid gap-3 sm:grid-cols-2">
                <ReadingBlock
                  icon={Zap}
                  label="Điện"
                  period={`${latestReading.billingMonth}/${latestReading.billingYear}`}
                  usage={`${latestReading.electricityUsage} kWh`}
                  amount={formatCurrency(latestReading.electricityUsage * latestReading.electricityUnitPrice)}
                />
                <ReadingBlock
                  icon={Home}
                  label="Nước"
                  period={`${latestReading.billingMonth}/${latestReading.billingYear}`}
                  usage={`${latestReading.waterUsage} m3`}
                  amount={formatCurrency(latestReading.waterUsage * latestReading.waterUnitPrice)}
                />
              </div>
            ) : (
              <p className="text-sm text-zinc-500">Chưa có chỉ số điện nước.</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Lịch sử sửa chữa</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data?.maintenanceRequests.slice(0, 5).map((item) => (
              <div key={item.id} className="rounded-xl border border-zinc-200 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="break-words font-medium text-zinc-950">{item.title}</p>
                    <p className="mt-1 break-words text-sm leading-6 text-zinc-500">{item.description}</p>
                  </div>
                  <Badge>{item.status}</Badge>
                </div>
              </div>
            ))}
            {data?.maintenanceRequests.length === 0 && (
              <p className="text-sm text-zinc-500">Chưa có yêu cầu sửa chữa.</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

type MetricCardProps = {
  icon: typeof Home;
  label: string;
  value: string;
  detail: string;
  tone: string;
};

function MetricCard({ icon: Icon, label, value, detail, tone }: MetricCardProps) {
  return (
    <Card>
      <CardContent className="min-h-36 p-4 sm:min-h-28 sm:p-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-50 sm:float-right">
          <Icon className={`h-5 w-5 ${tone}`} />
        </div>
        <div className="mt-3 min-w-0 sm:mt-0 sm:pr-12">
          <p className="text-xs leading-5 text-zinc-500 sm:text-sm">{label}</p>
          <p className="mt-1 break-words text-lg font-semibold leading-tight text-zinc-950 sm:mt-2 sm:text-2xl">{value}</p>
          {detail && <p className="mt-1 truncate text-xs text-zinc-500">{detail}</p>}
        </div>
      </CardContent>
    </Card>
  );
}

type ReadingBlockProps = {
  icon: typeof Home;
  label: string;
  period: string;
  usage: string;
  amount: string;
};

function ReadingBlock({ icon: Icon, label, period, usage, amount }: ReadingBlockProps) {
  return (
    <div className="rounded-xl border border-zinc-200 p-4">
      <div className="flex items-center gap-2 text-sm font-medium text-zinc-600">
        <Icon className="h-4 w-4 text-teal-700" />
        {label} kỳ {period}
      </div>
      <p className="mt-3 text-2xl font-semibold text-zinc-950">{usage}</p>
      <p className="mt-1 text-sm text-zinc-500">{amount}</p>
    </div>
  );
}
