import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Select } from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import type { RoomPayload } from "./propertyApi";

const schema = z.object({
  roomNumber: z.string().min(1),
  floorNumber: z.coerce.number().optional(),
  area: z.coerce.number().positive(),
  monthlyRent: z.coerce.number().min(0),
  defaultDeposit: z.coerce.number().min(0),
  maxOccupants: z.coerce.number().int().positive(),
  status: z.enum(["AVAILABLE", "OCCUPIED", "MAINTENANCE", "INACTIVE"]),
  description: z.string().optional(),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function RoomForm({
  disabled,
  onSubmit,
  isSubmitting,
}: {
  disabled: boolean;
  onSubmit: (payload: RoomPayload) => void;
  isSubmitting: boolean;
}) {
  const form = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      roomNumber: "",
      floorNumber: undefined,
      area: 20,
      monthlyRent: 0,
      defaultDeposit: 0,
      maxOccupants: 2,
      status: "AVAILABLE",
      description: "",
    },
  });

  return (
    <form
      className="space-y-4"
      onSubmit={form.handleSubmit((values) => {
        onSubmit(values);
        form.reset();
      })}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="room-number">Số phòng</Label>
          <Input id="room-number" disabled={disabled} {...form.register("roomNumber")} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="floor-number">Tầng</Label>
          <Input id="floor-number" type="number" disabled={disabled} {...form.register("floorNumber")} />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="area">Diện tích</Label>
          <Input id="area" type="number" step="0.01" disabled={disabled} {...form.register("area")} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="occupants">Số người</Label>
          <Input id="occupants" type="number" disabled={disabled} {...form.register("maxOccupants")} />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="rent">Giá thuê</Label>
          <Input id="rent" type="number" disabled={disabled} {...form.register("monthlyRent")} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="deposit">Tiền cọc</Label>
          <Input id="deposit" type="number" disabled={disabled} {...form.register("defaultDeposit")} />
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="status">Trạng thái</Label>
        <Select id="status" disabled={disabled} {...form.register("status")}>
          <option value="AVAILABLE">Trống</option>
          <option value="MAINTENANCE">Đang sửa</option>
          <option value="INACTIVE">Ngừng dùng</option>
        </Select>
      </div>
      <div className="space-y-2">
        <Label htmlFor="room-description">Mô tả</Label>
        <Textarea id="room-description" disabled={disabled} {...form.register("description")} />
      </div>
      <Button className="w-full sm:w-auto" disabled={disabled || isSubmitting}>
        <Plus className="h-4 w-4" />
        Thêm phòng
      </Button>
    </form>
  );
}
