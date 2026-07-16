import { useMutation, useQuery } from "@tanstack/react-query";
import { Home, Pencil, RefreshCw, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { PageHeader } from "../../components/ui/page-header";
import { Table, Td, Th } from "../../components/ui/table";
import { queryClient } from "../../lib/query-client/queryClient";
import { cn, formatCurrency } from "../../lib/utils";
import { createProperty, createRoom, listProperties, listRooms, updateProperty, updateRoom } from "./propertyApi";
import { PropertyForm } from "./PropertyForm";
import { RoomForm } from "./RoomForm";
import { usePropertyUiStore } from "./usePropertyUiStore";

export function PropertiesPage() {
  const { selectedPropertyId, setSelectedPropertyId } = usePropertyUiStore();
  const [editingProperty, setEditingProperty] = useState(false);
  const [editingRoomId, setEditingRoomId] = useState<number | null>(null);
  const propertiesQuery = useQuery({ queryKey: ["properties"], queryFn: listProperties });
  const selectedProperty = propertiesQuery.data?.find((property) => property.id === selectedPropertyId) ?? null;
  const roomsQuery = useQuery({
    queryKey: ["properties", selectedPropertyId, "rooms"],
    queryFn: () => listRooms(selectedPropertyId!),
    enabled: selectedPropertyId != null,
  });

  useEffect(() => {
    if (!selectedPropertyId && propertiesQuery.data?.length) {
      setSelectedPropertyId(propertiesQuery.data[0].id);
    }
  }, [propertiesQuery.data, selectedPropertyId, setSelectedPropertyId]);

  const createPropertyMutation = useMutation({
    mutationFn: createProperty,
    onSuccess: (property) => {
      queryClient.invalidateQueries({ queryKey: ["properties"] });
      setSelectedPropertyId(property.id);
    },
  });

  const createRoomMutation = useMutation({
    mutationFn: (payload: Parameters<typeof createRoom>[1]) => createRoom(selectedPropertyId!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["properties", selectedPropertyId, "rooms"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "summary"] });
    },
  });

  const updatePropertyMutation = useMutation({
    mutationFn: (payload: Parameters<typeof updateProperty>[1]) => updateProperty(selectedPropertyId!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["properties"] });
      setEditingProperty(false);
    },
  });

  const updateRoomMutation = useMutation({
    mutationFn: (payload: Parameters<typeof updateRoom>[1]) => updateRoom(editingRoomId!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["properties", selectedPropertyId, "rooms"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "summary"] });
      setEditingRoomId(null);
    },
  });

  const rooms = roomsQuery.data ?? [];
  const editingRoom = rooms.find((room) => room.id === editingRoomId) ?? null;

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Tài sản cho thuê"
        title="Khu trọ và phòng"
        description="Quản lý các khu trọ, theo dõi phòng và thêm phòng mới."
        action={
          <Button variant="secondary" onClick={() => propertiesQuery.refetch()}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
        }
      />

      <div className="grid gap-5 xl:grid-cols-[360px_1fr]">
        <div className="contents xl:block xl:space-y-5">
          <Card className="order-3">
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle>{editingProperty ? "Cập nhật khu trọ" : "Thêm khu trọ"}</CardTitle>
              {editingProperty && <Button variant="ghost" size="icon" title="Hủy chỉnh sửa" onClick={() => setEditingProperty(false)}><X className="h-4 w-4" /></Button>}
            </CardHeader>
            <CardContent>
              <PropertyForm
                key={editingProperty ? `edit-${selectedPropertyId}` : "create-property"}
                initialValues={editingProperty && selectedProperty ? selectedProperty : undefined}
                isSubmitting={createPropertyMutation.isPending || updatePropertyMutation.isPending}
                onSubmit={(payload) => editingProperty
                  ? updatePropertyMutation.mutateAsync(payload)
                  : createPropertyMutation.mutateAsync(payload)}
              />
            </CardContent>
          </Card>

          <Card className="order-1">
            <CardHeader>
              <CardTitle>Danh sách khu trọ</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {propertiesQuery.data?.map((property) => (
                <button
                  key={property.id}
                  className={cn(
                    "flex min-h-14 w-full touch-manipulation items-start gap-3 rounded-xl border border-zinc-200 p-3 text-left transition hover:bg-zinc-50",
                    selectedPropertyId === property.id && "border-teal-500 bg-teal-50",
                  )}
                  onClick={() => setSelectedPropertyId(property.id)}
                >
                  <Home className="mt-0.5 h-4 w-4 text-teal-700" />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium text-zinc-950">{property.name}</span>
                    <span className="block truncate text-xs text-zinc-500">{property.provinceCity}</span>
                  </span>
                  <Badge>{property.status}</Badge>
                </button>
              ))}
              {!propertiesQuery.data?.length && <p className="text-sm text-zinc-500">Chưa có khu trọ</p>}
            </CardContent>
          </Card>
        </div>

        <div className="contents xl:block xl:space-y-5">
          <Card className="order-2">
            <CardHeader className="flex-row items-center justify-between">
              <div><CardTitle>{selectedProperty ? selectedProperty.name : "Phòng"}</CardTitle>{selectedProperty && <p className="mt-1 text-sm text-zinc-500">{selectedProperty.addressLine}, {selectedProperty.provinceCity}</p>}</div>
              {selectedProperty && <Button variant="secondary" size="sm" onClick={() => setEditingProperty(true)}><Pencil className="h-4 w-4" />Sửa khu trọ</Button>}
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="space-y-3 sm:hidden">
                {rooms.map((room) => (
                  <div key={room.id} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-zinc-950">Phòng {room.roomNumber}</p>
                        <p className="mt-1 text-sm text-zinc-500">{room.area} m² · tối đa {room.maxOccupants} người</p>
                      </div>
                      <Badge>{room.status}</Badge>
                    </div>
                    <div className="mt-4 rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Giá thuê</p>
                      <p className="mt-1 text-sm font-semibold text-zinc-950">{formatCurrency(room.monthlyRent)}</p>
                    </div>
                    <Button className="mt-3 w-full" variant="secondary" size="sm" onClick={() => setEditingRoomId(room.id)}><Pencil className="h-4 w-4" />Sửa phòng</Button>
                  </div>
                ))}
                {selectedPropertyId && rooms.length === 0 && (
                  <div className="rounded-2xl border border-dashed border-zinc-300 bg-white p-6 text-center text-sm text-zinc-500">
                    Chưa có phòng
                  </div>
                )}
              </div>

              <div className="hidden overflow-x-auto sm:block">
                <Table>
                  <thead>
                    <tr>
                      <Th>Phòng</Th>
                      <Th>Diện tích</Th>
                      <Th>Giá thuê</Th>
                      <Th>Số người</Th>
                      <Th>Trạng thái</Th>
                      <Th>Thao tác</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {rooms.map((room) => (
                      <tr key={room.id}>
                        <Td className="font-medium text-zinc-950">{room.roomNumber}</Td>
                        <Td>{room.area} m2</Td>
                        <Td>{formatCurrency(room.monthlyRent)}</Td>
                        <Td>{room.maxOccupants}</Td>
                        <Td>
                          <Badge>{room.status}</Badge>
                        </Td>
                        <Td><Button variant="ghost" size="icon" title="Sửa phòng" onClick={() => setEditingRoomId(room.id)}><Pencil className="h-4 w-4" /></Button></Td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            </CardContent>
          </Card>

          <Card className="order-4">
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle>{editingRoom ? `Cập nhật phòng ${editingRoom.roomNumber}` : "Thêm phòng"}</CardTitle>
              {editingRoom && <Button variant="ghost" size="icon" title="Hủy chỉnh sửa" onClick={() => setEditingRoomId(null)}><X className="h-4 w-4" /></Button>}
            </CardHeader>
            <CardContent>
              <RoomForm
                key={editingRoom ? `edit-room-${editingRoom.id}` : `create-room-${selectedPropertyId}`}
                disabled={!selectedPropertyId}
                initialValues={editingRoom ?? undefined}
                isSubmitting={createRoomMutation.isPending || updateRoomMutation.isPending}
                onSubmit={(payload) => editingRoom
                  ? updateRoomMutation.mutateAsync(payload)
                  : createRoomMutation.mutateAsync(payload)}
              />
            </CardContent>
          </Card>
        </div>
      </div>
      {(createPropertyMutation.isError || updatePropertyMutation.isError || createRoomMutation.isError || updateRoomMutation.isError) && (
        <p className="text-sm text-red-600">Không thể lưu dữ liệu. Hãy kiểm tra trạng thái phòng và thông tin đã nhập.</p>
      )}
    </div>
  );
}
