import { useEffect, useMemo, useState } from "react";
import { CalendarX2, RefreshCw } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { PageHeader } from "../../components/ui/page-header";
import { Label } from "../../components/ui/label";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { Textarea } from "../../components/ui/textarea";
import { useMutation, useQuery } from "@tanstack/react-query";
import { queryClient } from "../../lib/query-client/queryClient";
import { formatCurrency } from "../../lib/utils";
import { createContract, endContract, listContracts, type ContractPayload } from "./contractApi";
import { listProperties, listRooms } from "../properties/propertyApi";
import { listTenants } from "../tenants/tenantApi";

const initialFormState: Omit<ContractPayload, "tenantIds" | "primaryTenantId"> & {
  propertyId?: number;
  tenantIds: number[];
  primaryTenantId: number | null;
} = {
  roomId: 0,
  propertyId: undefined,
  contractCode: "",
  startDate: "",
  endDate: "",
  monthlyRent: 0,
  depositAmount: 0,
  status: "ACTIVE",
  tenantIds: [],
  primaryTenantId: null,
  notes: "",
};

export function ContractsPage() {
  const contractsQuery = useQuery({ queryKey: ["contracts"], queryFn: listContracts });
  const propertiesQuery = useQuery({ queryKey: ["properties"], queryFn: listProperties });
  const tenantsQuery = useQuery({ queryKey: ["tenants"], queryFn: listTenants });
  const [form, setForm] = useState(initialFormState);

  const selectedProperty = propertiesQuery.data?.find((property) => property.id === form.propertyId) ?? null;
  const roomsQuery = useQuery({
    queryKey: ["properties", form.propertyId, "rooms"],
    queryFn: () => (form.propertyId ? listRooms(form.propertyId) : Promise.resolve([])),
    enabled: Boolean(form.propertyId),
  });

  useEffect(() => {
    if (!form.propertyId && propertiesQuery.data?.length) {
      setForm((current) => ({ ...current, propertyId: propertiesQuery.data![0].id }));
    }
  }, [propertiesQuery.data, form.propertyId]);

  useEffect(() => {
    if (form.roomId && roomsQuery.data) {
      const room = roomsQuery.data.find((item) => item.id === form.roomId);
      if (room && !form.monthlyRent) {
        setForm((current) => ({ ...current, monthlyRent: room.monthlyRent }));
      }
    }
  }, [form.roomId, form.monthlyRent, roomsQuery.data]);

  const roomOptions = (roomsQuery.data ?? []).filter((room) => room.status === "AVAILABLE");
  const tenantOptions = (tenantsQuery.data ?? []).filter((tenant) => tenant.status === "ACTIVE");

  const primaryTenantOptions = useMemo(
    () => tenantOptions.filter((tenant) => form.tenantIds.includes(tenant.id)),
    [form.tenantIds, tenantOptions],
  );

  const createContractMutation = useMutation({
    mutationFn: createContract,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["contracts"] });
      setForm(initialFormState);
    },
  });
  const endContractMutation = useMutation({
    mutationFn: (id: number) => endContract(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["contracts"] });
      queryClient.invalidateQueries({ queryKey: ["properties"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "summary"] });
    },
  });

  function handleTenantToggle(tenantId: number) {
    setForm((current) => {
      const tenantIds = current.tenantIds.includes(tenantId)
        ? current.tenantIds.filter((id) => id !== tenantId)
        : [...current.tenantIds, tenantId];
      const primaryTenantId = tenantIds.includes(current.primaryTenantId ?? -1)
        ? current.primaryTenantId
        : tenantIds[0] ?? null;
      return { ...current, tenantIds, primaryTenantId };
    });
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!form.propertyId || !form.roomId || !form.startDate || form.monthlyRent < 0 || form.depositAmount < 0 || !form.tenantIds.length || !form.primaryTenantId) {
      return;
    }
    createContractMutation.mutate({
      roomId: form.roomId,
      contractCode: form.contractCode || undefined,
      startDate: form.startDate,
      endDate: form.endDate || undefined,
      monthlyRent: Number(form.monthlyRent),
      depositAmount: Number(form.depositAmount),
      status: form.status,
      tenantIds: form.tenantIds,
      primaryTenantId: form.primaryTenantId,
      notes: form.notes || undefined,
    });
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Quản lý hợp đồng"
        title="Hợp đồng thuê"
        description="Tạo hợp đồng mới và xem danh sách hợp đồng hiện tại."
        action={
          <Button variant="secondary" onClick={() => contractsQuery.refetch()}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
        }
      />

      <div className="grid gap-5 xl:grid-cols-[420px_1fr]">
        <Card className="order-2 xl:order-1">
          <CardHeader>
            <CardTitle>Thêm hợp đồng</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="property">Khu trọ</Label>
                  <Select
                    id="property"
                    value={form.propertyId ?? ""}
                    onChange={(event) => setForm({ ...form, propertyId: Number(event.target.value), roomId: 0 })}
                  >
                    {propertiesQuery.data?.map((property) => (
                      <option key={property.id} value={property.id}>
                        {property.name}
                      </option>
                    ))}
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="room">Phòng</Label>
                  <Select
                    id="room"
                    value={form.roomId ?? ""}
                    onChange={(event) => setForm({ ...form, roomId: Number(event.target.value) })}
                    disabled={!roomOptions.length}
                  >
                    <option value="">Chọn phòng</option>
                    {roomOptions.map((room) => (
                      <option key={room.id} value={room.id}>
                        {room.roomNumber} · {room.area}m2
                      </option>
                    ))}
                  </Select>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="startDate">Ngày bắt đầu</Label>
                  <Input
                    id="startDate"
                    type="date"
                    value={form.startDate}
                    onChange={(event) => setForm({ ...form, startDate: event.target.value })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="endDate">Ngày kết thúc</Label>
                  <Input
                    id="endDate"
                    type="date"
                    value={form.endDate}
                    onChange={(event) => setForm({ ...form, endDate: event.target.value })}
                  />
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="monthlyRent">Giá thuê</Label>
                  <Input
                    id="monthlyRent"
                    type="number"
                    min={0}
                    value={form.monthlyRent}
                    onChange={(event) => setForm({ ...form, monthlyRent: Number(event.target.value) })}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="depositAmount">Tiền đặt cọc</Label>
                  <Input
                    id="depositAmount"
                    type="number"
                    min={0}
                    value={form.depositAmount}
                    onChange={(event) => setForm({ ...form, depositAmount: Number(event.target.value) })}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="contractCode">Mã hợp đồng</Label>
                <Input
                  id="contractCode"
                  value={form.contractCode}
                  onChange={(event) => setForm({ ...form, contractCode: event.target.value })}
                  placeholder="Tự động nếu bỏ trống"
                />
              </div>

              <div className="space-y-2">
                <Label>Người thuê</Label>
                <div className="grid gap-2">
                  {tenantOptions.map((tenant) => (
                    <label key={tenant.id} className="flex min-h-12 touch-manipulation items-center gap-3 rounded-xl border border-zinc-200 p-3">
                      <input
                        type="checkbox"
                        checked={form.tenantIds.includes(tenant.id)}
                        onChange={() => handleTenantToggle(tenant.id)}
                        className="h-4 w-4 rounded border-zinc-300 text-teal-600"
                      />
                      <span className="text-sm text-zinc-700">{tenant.fullName} ({tenant.phone ?? "không có"})</span>
                    </label>
                  ))}
                  {!tenantOptions.length && <p className="text-sm text-zinc-500">Chưa có người thuê để chọn.</p>}
                </div>
              </div>

              {primaryTenantOptions.length > 0 && (
                <div className="space-y-2">
                  <Label htmlFor="primaryTenant">Người thuê chính</Label>
                  <Select
                    id="primaryTenant"
                    value={form.primaryTenantId ?? ""}
                    onChange={(event) => setForm({ ...form, primaryTenantId: Number(event.target.value) })}
                  >
                    {primaryTenantOptions.map((tenant) => (
                      <option key={tenant.id} value={tenant.id}>
                        {tenant.fullName}
                      </option>
                    ))}
                  </Select>
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="notes">Ghi chú</Label>
                <Textarea
                  id="notes"
                  value={form.notes}
                  onChange={(event) => setForm({ ...form, notes: event.target.value })}
                  rows={4}
                />
              </div>

              <Button className="w-full" disabled={createContractMutation.isPending || !tenantOptions.length || !roomOptions.length}>
                {createContractMutation.isPending ? "Đang tạo..." : "Tạo hợp đồng"}
              </Button>
              {createContractMutation.isError && (
                <p className="text-sm text-red-600">Không thể tạo hợp đồng. Vui lòng kiểm tra dữ liệu và thử lại.</p>
              )}
            </form>
          </CardContent>
        </Card>

        <Card className="order-1 xl:order-2">
          <CardHeader>
            <CardTitle>Danh sách hợp đồng</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="hidden overflow-x-auto sm:block">
              <Table>
                <thead>
                  <tr>
                    <Th>Mã hợp đồng</Th>
                    <Th>Phòng</Th>
                    <Th>Ngày bắt đầu</Th>
                    <Th>Ngày kết thúc</Th>
                    <Th>Giá thuê</Th>
                    <Th>Trạng thái</Th>
                    <Th>Thao tác</Th>
                  </tr>
                </thead>
                <tbody>
                  {(contractsQuery.data ?? []).map((contract) => (
                    <tr key={contract.id}>
                      <Td className="font-medium text-zinc-950">{contract.contractCode}</Td>
                      <Td>{contract.roomNumber}</Td>
                      <Td>{contract.startDate}</Td>
                      <Td>{contract.endDate ?? "—"}</Td>
                      <Td>{formatCurrency(Number(contract.monthlyRent))}</Td>
                      <Td>
                        <Badge>{contract.status}</Badge>
                      </Td>
                      <Td>
                        {contract.status === "ACTIVE" && (
                          <Button variant="secondary" size="sm" disabled={endContractMutation.isPending} onClick={() => endContractMutation.mutate(contract.id)}>
                            <CalendarX2 className="h-4 w-4" />Kết thúc
                          </Button>
                        )}
                      </Td>
                    </tr>
                  ))}
                  {contractsQuery.data?.length === 0 && (
                    <tr>
                      <Td colSpan={6} className="text-zinc-500">
                        Chưa có hợp đồng.
                      </Td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            <div className="space-y-3 sm:hidden">
              {(contractsQuery.data ?? []).map((contract) => (
                <div key={contract.id} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-zinc-950">{contract.contractCode}</p>
                      <p className="text-sm text-zinc-500">Phòng {contract.roomNumber}</p>
                    </div>
                    <Badge>{contract.status}</Badge>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2">
                    <div className="rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Bắt đầu</p>
                      <p className="mt-1 text-sm text-zinc-950">{contract.startDate}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Kết thúc</p>
                      <p className="mt-1 text-sm text-zinc-950">{contract.endDate ?? "—"}</p>
                    </div>
                    <div className="col-span-2 rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Giá thuê</p>
                      <p className="mt-1 text-sm font-semibold text-zinc-950">{formatCurrency(Number(contract.monthlyRent))}</p>
                    </div>
                    {contract.status === "ACTIVE" && (
                      <Button className="col-span-2" variant="secondary" disabled={endContractMutation.isPending} onClick={() => endContractMutation.mutate(contract.id)}>
                        <CalendarX2 className="h-4 w-4" />Kết thúc hợp đồng
                      </Button>
                    )}
                  </div>
                </div>
              ))}
              {!contractsQuery.data?.length && (
                <div className="rounded-2xl border border-dashed border-zinc-200 bg-white p-6 text-center text-sm text-zinc-500">
                  Chưa có hợp đồng.
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
      {endContractMutation.isError && <p className="text-sm text-red-600">Không thể kết thúc hợp đồng. Hãy kiểm tra ngày bắt đầu.</p>}
    </div>
  );
}
