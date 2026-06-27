import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { PageHeader } from "../../components/ui/page-header";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { useMutation, useQuery } from "@tanstack/react-query";
import { queryClient } from "../../lib/query-client/queryClient";
import { listMaintenanceRequests, updateMaintenanceStatus, type MaintenanceStatus, type MaintenanceStatusUpdatePayload } from "./maintenanceApi";

const statusOptions: MaintenanceStatus[] = ["PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export function MaintenancePage() {
  const maintenanceQuery = useQuery({ queryKey: ["maintenance-requests"], queryFn: listMaintenanceRequests });
  const [statusChanges, setStatusChanges] = useState<Record<number, MaintenanceStatus>>({});
  const [notesChanges, setNotesChanges] = useState<Record<number, string>>({});

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: MaintenanceStatusUpdatePayload }) => updateMaintenanceStatus(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["maintenance-requests"] }),
  });

  const rows = maintenanceQuery.data ?? [];

  function handleStatusChange(id: number, status: MaintenanceStatus) {
    setStatusChanges((current) => ({ ...current, [id]: status }));
  }

  function handleNotesChange(id: number, notes: string) {
    setNotesChanges((current) => ({ ...current, [id]: notes }));
  }

  function submitStatus(id: number) {
    const payload: MaintenanceStatusUpdatePayload = {
      status: statusChanges[id] ?? rows.find((row) => row.id === id)?.status ?? "PENDING",
      resolutionNotes: notesChanges[id] || undefined,
    };
    updateStatusMutation.mutate({ id, payload });
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Bảo trì"
        title="Yêu cầu sửa chữa"
        description="Theo dõi và cập nhật trạng thái yêu cầu sửa chữa của khách thuê."
        action={
          <Button variant="secondary" onClick={() => maintenanceQuery.refetch()}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Danh sách yêu cầu sửa chữa</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="hidden sm:block overflow-x-auto">
            <Table>
              <thead>
                <tr>
                  <Th>Tiêu đề</Th>
                  <Th>Phòng</Th>
                  <Th>Người gửi</Th>
                  <Th>Ưu tiên</Th>
                  <Th>Trạng thái</Th>
                  <Th>Hành động</Th>
                </tr>
              </thead>
              <tbody>
                {rows.map((request) => (
                  <tr key={request.id}>
                    <Td className="font-medium text-zinc-950">{request.title}</Td>
                    <Td>{request.roomNumber}</Td>
                    <Td>{request.tenantName ?? "Chủ"}</Td>
                    <Td>
                      <Badge>{request.priority}</Badge>
                    </Td>
                    <Td>
                      <Select
                        value={statusChanges[request.id] ?? request.status}
                        onChange={(event) => handleStatusChange(request.id, event.target.value as MaintenanceStatus)}
                      >
                        {statusOptions.map((status) => (
                          <option key={status} value={status}>
                            {status}
                          </option>
                        ))}
                      </Select>
                    </Td>
                    <Td>
                      <div className="flex flex-col gap-2">
                        <textarea
                          className="min-h-[4rem] rounded-md border border-zinc-200 bg-white p-2 text-sm text-zinc-700 outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                          placeholder="Ghi chú xử lý"
                          value={notesChanges[request.id] ?? ""}
                          onChange={(event) => handleNotesChange(request.id, event.target.value)}
                        />
                        <Button
                          type="button"
                          size="sm"
                          disabled={updateStatusMutation.isPending}
                          onClick={() => submitStatus(request.id)}
                        >
                          Cập nhật
                        </Button>
                      </div>
                    </Td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <Td colSpan={6} className="text-zinc-500">
                      Chưa có yêu cầu sửa chữa.
                    </Td>
                  </tr>
                )}
              </tbody>
            </Table>
          </div>
          <div className="space-y-4 sm:hidden">
            {rows.map((request) => (
              <div key={request.id} className="rounded-3xl border border-zinc-200 bg-zinc-50 p-4 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-zinc-950">{request.title}</p>
                    <p className="mt-1 text-sm text-zinc-500">Phòng {request.roomNumber}</p>
                  </div>
                  <Badge>{request.priority}</Badge>
                </div>
                <div className="mt-4 space-y-3">
                  <div className="rounded-2xl bg-white p-3">
                    <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">Người gửi</p>
                    <p className="mt-1 text-sm text-zinc-950">{request.tenantName ?? "Chủ"}</p>
                  </div>
                  <div className="rounded-2xl bg-white p-3">
                    <p className="text-xs uppercase tracking-[0.18em] text-zinc-500">Trạng thái</p>
                    <Select
                      value={statusChanges[request.id] ?? request.status}
                      onChange={(event) => handleStatusChange(request.id, event.target.value as MaintenanceStatus)}
                    >
                      {statusOptions.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2 rounded-2xl bg-white p-3">
                    <label className="text-xs uppercase tracking-[0.18em] text-zinc-500">Ghi chú xử lý</label>
                    <textarea
                      className="min-h-[4rem] w-full rounded-md border border-zinc-200 bg-white p-2 text-sm text-zinc-700 outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                      placeholder="Ghi chú xử lý"
                      value={notesChanges[request.id] ?? ""}
                      onChange={(event) => handleNotesChange(request.id, event.target.value)}
                    />
                    <Button
                      type="button"
                      size="sm"
                      disabled={updateStatusMutation.isPending}
                      onClick={() => submitStatus(request.id)}
                    >
                      Cập nhật
                    </Button>
                  </div>
                </div>
              </div>
            ))}
            {!rows.length && (
              <div className="rounded-3xl border border-zinc-200 bg-white p-6 text-center text-sm text-zinc-500">
                Chưa có yêu cầu sửa chữa.
              </div>
            )}
          </div>
        </CardContent>
      </Card>
      {updateStatusMutation.isError && (
        <p className="text-sm text-red-600">Không thể cập nhật trạng thái. Vui lòng thử lại.</p>
      )}
    </div>
  );
}
