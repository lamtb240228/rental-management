import { useMutation, useQuery } from "@tanstack/react-query";
import { Home, RefreshCw } from "lucide-react";
import { useEffect } from "react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { PageHeader } from "../../components/ui/page-header";
import { Table, Td, Th } from "../../components/ui/table";
import { queryClient } from "../../lib/query-client/queryClient";
import { cn, formatCurrency } from "../../lib/utils";
import { createProperty, createRoom, listProperties, listRooms } from "./propertyApi";
import { PropertyForm } from "./PropertyForm";
import { RoomForm } from "./RoomForm";
import { usePropertyUiStore } from "./usePropertyUiStore";

export function PropertiesPage() {
  const { selectedPropertyId, setSelectedPropertyId } = usePropertyUiStore();
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

  const rooms = roomsQuery.data ?? [];

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
            <CardHeader>
              <CardTitle>Thêm khu trọ</CardTitle>
            </CardHeader>
            <CardContent>
              <PropertyForm
                isSubmitting={createPropertyMutation.isPending}
                onSubmit={(payload) => createPropertyMutation.mutate(payload)}
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
            <CardHeader>
              <CardTitle>{selectedProperty ? selectedProperty.name : "Phòng"}</CardTitle>
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
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </div>
            </CardContent>
          </Card>

          <Card className="order-4">
            <CardHeader>
              <CardTitle>Thêm phòng</CardTitle>
            </CardHeader>
            <CardContent>
              <RoomForm
                disabled={!selectedPropertyId}
                isSubmitting={createRoomMutation.isPending}
                onSubmit={(payload) => createRoomMutation.mutate(payload)}
              />
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
