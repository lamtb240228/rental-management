import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Gauge, Pencil, RefreshCw, X } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { PageHeader } from "../../components/ui/page-header";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { queryClient } from "../../lib/query-client/queryClient";
import { formatCurrency } from "../../lib/utils";
import { listProperties, listRooms } from "../properties/propertyApi";
import {
  createUtilityReading,
  listUtilityReadings,
  updateUtilityReading,
  type UtilityReadingPayload,
} from "./utilityApi";

function emptyReading(): UtilityReadingPayload {
  const now = new Date();
  return {
    billingYear: now.getFullYear(),
    billingMonth: now.getMonth() + 1,
    electricityOldReading: 0,
    electricityNewReading: 0,
    electricityUnitPrice: 3500,
    waterOldReading: 0,
    waterNewReading: 0,
    waterUnitPrice: 20000,
  };
}

export function UtilitiesPage() {
  const [propertyId, setPropertyId] = useState<number | null>(null);
  const [roomId, setRoomId] = useState<number | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<UtilityReadingPayload>(emptyReading);

  const propertiesQuery = useQuery({ queryKey: ["properties"], queryFn: listProperties });
  const roomsQuery = useQuery({
    queryKey: ["properties", propertyId, "rooms"],
    queryFn: () => listRooms(propertyId!),
    enabled: propertyId != null,
  });
  const readingsQuery = useQuery({
    queryKey: ["utility-readings", roomId],
    queryFn: () => listUtilityReadings(roomId!),
    enabled: roomId != null,
  });

  useEffect(() => {
    if (propertyId == null && propertiesQuery.data?.length) {
      setPropertyId(propertiesQuery.data[0].id);
    }
  }, [propertiesQuery.data, propertyId]);

  useEffect(() => {
    const rooms = roomsQuery.data ?? [];
    if (!rooms.some((room) => room.id === roomId)) {
      setRoomId(rooms[0]?.id ?? null);
    }
  }, [roomId, roomsQuery.data]);

  useEffect(() => {
    if (editingId || !readingsQuery.data?.length) return;
    const latest = readingsQuery.data[0];
    setForm((current) => ({
      ...current,
      electricityOldReading: Number(latest.electricityNewReading),
      electricityNewReading: Number(latest.electricityNewReading),
      electricityUnitPrice: Number(latest.electricityUnitPrice),
      waterOldReading: Number(latest.waterNewReading),
      waterNewReading: Number(latest.waterNewReading),
      waterUnitPrice: Number(latest.waterUnitPrice),
    }));
  }, [editingId, readingsQuery.data, roomId]);

  const saveMutation = useMutation({
    mutationFn: () => editingId
      ? updateUtilityReading(editingId, form)
      : createUtilityReading(roomId!, form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["utility-readings", roomId] });
      setEditingId(null);
      setForm(emptyReading());
    },
  });

  const electricityUsage = Math.max(0, form.electricityNewReading - form.electricityOldReading);
  const waterUsage = Math.max(0, form.waterNewReading - form.waterOldReading);
  const estimatedTotal = electricityUsage * form.electricityUnitPrice + waterUsage * form.waterUnitPrice;
  const selectedRoom = useMemo(
    () => roomsQuery.data?.find((room) => room.id === roomId),
    [roomId, roomsQuery.data],
  );

  function beginEdit(reading: NonNullable<typeof readingsQuery.data>[number]) {
    setEditingId(reading.id);
    setForm({
      billingYear: reading.billingYear,
      billingMonth: reading.billingMonth,
      electricityOldReading: Number(reading.electricityOldReading),
      electricityNewReading: Number(reading.electricityNewReading),
      electricityUnitPrice: Number(reading.electricityUnitPrice),
      waterOldReading: Number(reading.waterOldReading),
      waterNewReading: Number(reading.waterNewReading),
      waterUnitPrice: Number(reading.waterUnitPrice),
    });
  }

  function setNumber(key: keyof UtilityReadingPayload, value: string) {
    setForm((current) => ({ ...current, [key]: Number(value) }));
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Vận hành hàng tháng"
        title="Chỉ số điện nước"
        description="Ghi chỉ số theo phòng, kiểm tra mức sử dụng và đơn giá trước khi lập hóa đơn."
        action={<Button variant="secondary" onClick={() => readingsQuery.refetch()}><RefreshCw className="h-4 w-4" />Tải lại</Button>}
      />

      <div className="grid gap-5 xl:grid-cols-[390px_1fr]">
        <Card>
          <CardHeader><CardTitle>{editingId ? "Cập nhật chỉ số" : "Ghi chỉ số mới"}</CardTitle></CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); saveMutation.mutate(); }}>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
                <div className="space-y-2">
                  <Label htmlFor="utility-property">Khu trọ</Label>
                  <Select id="utility-property" value={propertyId ?? ""} onChange={(event) => { setPropertyId(Number(event.target.value)); setRoomId(null); }}>
                    {propertiesQuery.data?.map((property) => <option key={property.id} value={property.id}>{property.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="utility-room">Phòng</Label>
                  <Select id="utility-room" value={roomId ?? ""} onChange={(event) => { setRoomId(Number(event.target.value)); setEditingId(null); }}>
                    {(roomsQuery.data ?? []).map((room) => <option key={room.id} value={room.id}>{room.roomNumber}</option>)}
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2"><Label htmlFor="billing-month">Tháng</Label><Input id="billing-month" type="number" min={1} max={12} value={form.billingMonth} onChange={(e) => setNumber("billingMonth", e.target.value)} /></div>
                <div className="space-y-2"><Label htmlFor="billing-year">Năm</Label><Input id="billing-year" type="number" min={2000} value={form.billingYear} onChange={(e) => setNumber("billingYear", e.target.value)} /></div>
              </div>

              <ReadingFields label="Điện" prefix="electricity" form={form} setNumber={setNumber} />
              <ReadingFields label="Nước" prefix="water" form={form} setNumber={setNumber} />

              <div className="rounded-lg border border-zinc-200 bg-slate-50 p-4 text-sm">
                <div className="flex justify-between"><span className="text-zinc-500">Điện tiêu thụ</span><strong>{electricityUsage}</strong></div>
                <div className="mt-2 flex justify-between"><span className="text-zinc-500">Nước tiêu thụ</span><strong>{waterUsage}</strong></div>
                <div className="mt-3 flex justify-between border-t border-zinc-200 pt-3"><span className="text-zinc-700">Tạm tính</span><strong>{formatCurrency(estimatedTotal)}</strong></div>
              </div>

              <div className="flex gap-2">
                <Button className="flex-1" disabled={!roomId || saveMutation.isPending}><Gauge className="h-4 w-4" />{editingId ? "Lưu thay đổi" : "Lưu chỉ số"}</Button>
                {editingId && <Button type="button" variant="secondary" size="icon" title="Hủy chỉnh sửa" onClick={() => { setEditingId(null); setForm(emptyReading()); }}><X className="h-4 w-4" /></Button>}
              </div>
              {saveMutation.isError && <p className="text-sm text-red-600">Không thể lưu. Hãy kiểm tra kỳ và chỉ số mới.</p>}
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Lịch sử {selectedRoom ? `phòng ${selectedRoom.roomNumber}` : "theo phòng"}</CardTitle></CardHeader>
          <CardContent>
            <div className="hidden overflow-x-auto sm:block">
              <Table><thead><tr><Th>Kỳ</Th><Th>Điện</Th><Th>Tiền điện</Th><Th>Nước</Th><Th>Tiền nước</Th><Th></Th></tr></thead>
                <tbody>{(readingsQuery.data ?? []).map((reading) => (
                  <tr key={reading.id}>
                    <Td className="font-medium">{reading.billingMonth}/{reading.billingYear}</Td>
                    <Td>{reading.electricityOldReading} → {reading.electricityNewReading} <Badge>{reading.electricityUsage}</Badge></Td>
                    <Td>{formatCurrency(Number(reading.electricityUsage) * Number(reading.electricityUnitPrice))}</Td>
                    <Td>{reading.waterOldReading} → {reading.waterNewReading} <Badge>{reading.waterUsage}</Badge></Td>
                    <Td>{formatCurrency(Number(reading.waterUsage) * Number(reading.waterUnitPrice))}</Td>
                    <Td><Button variant="ghost" size="icon" title="Chỉnh sửa" onClick={() => beginEdit(reading)}><Pencil className="h-4 w-4" /></Button></Td>
                  </tr>
                ))}</tbody>
              </Table>
            </div>
            <div className="space-y-3 sm:hidden">
              {(readingsQuery.data ?? []).map((reading) => (
                <div key={reading.id} className="rounded-lg border border-zinc-200 p-4">
                  <div className="flex items-center justify-between"><strong>{reading.billingMonth}/{reading.billingYear}</strong><Button variant="ghost" size="icon" onClick={() => beginEdit(reading)}><Pencil className="h-4 w-4" /></Button></div>
                  <div className="mt-3 grid grid-cols-2 gap-2 text-sm"><div className="rounded-lg bg-slate-50 p-3"><span className="text-zinc-500">Điện</span><p className="mt-1 font-medium">{reading.electricityUsage} · {formatCurrency(Number(reading.electricityUsage) * Number(reading.electricityUnitPrice))}</p></div><div className="rounded-lg bg-slate-50 p-3"><span className="text-zinc-500">Nước</span><p className="mt-1 font-medium">{reading.waterUsage} · {formatCurrency(Number(reading.waterUsage) * Number(reading.waterUnitPrice))}</p></div></div>
                </div>
              ))}
            </div>
            {roomId && readingsQuery.data?.length === 0 && <p className="py-8 text-center text-sm text-zinc-500">Phòng này chưa có chỉ số điện nước.</p>}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ReadingFields({ label, prefix, form, setNumber }: {
  label: string;
  prefix: "electricity" | "water";
  form: UtilityReadingPayload;
  setNumber: (key: keyof UtilityReadingPayload, value: string) => void;
}) {
  const oldKey = `${prefix}OldReading` as keyof UtilityReadingPayload;
  const newKey = `${prefix}NewReading` as keyof UtilityReadingPayload;
  const priceKey = `${prefix}UnitPrice` as keyof UtilityReadingPayload;
  return (
    <fieldset className="space-y-3 rounded-lg border border-zinc-200 p-4">
      <legend className="px-1 text-sm font-semibold text-zinc-900">{label}</legend>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-2"><Label>Chỉ số cũ</Label><Input type="number" min={0} step="0.01" value={form[oldKey]} onChange={(e) => setNumber(oldKey, e.target.value)} /></div>
        <div className="space-y-2"><Label>Chỉ số mới</Label><Input type="number" min={0} step="0.01" value={form[newKey]} onChange={(e) => setNumber(newKey, e.target.value)} /></div>
      </div>
      <div className="space-y-2"><Label>Đơn giá</Label><Input type="number" min={0} value={form[priceKey]} onChange={(e) => setNumber(priceKey, e.target.value)} /></div>
    </fieldset>
  );
}
