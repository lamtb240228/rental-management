import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import type { PropertyPayload } from "./propertyApi";

const schema = z.object({
  name: z.string().min(2),
  addressLine: z.string().min(2),
  ward: z.string().optional(),
  district: z.string().optional(),
  provinceCity: z.string().min(2),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function PropertyForm({
  onSubmit,
  isSubmitting,
}: {
  onSubmit: (payload: PropertyPayload) => void;
  isSubmitting: boolean;
}) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: "",
      addressLine: "",
      ward: "",
      district: "",
      provinceCity: "",
      description: "",
    },
  });

  return (
    <form
      className="space-y-4"
      onSubmit={form.handleSubmit((values) => {
        onSubmit({ ...values, status: "ACTIVE" });
        form.reset();
      })}
    >
      <div className="space-y-2">
        <Label htmlFor="property-name">Tên khu trọ</Label>
        <Input id="property-name" {...form.register("name")} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="address">Địa chỉ</Label>
        <Input id="address" {...form.register("addressLine")} />
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="ward">Phường xã</Label>
          <Input id="ward" {...form.register("ward")} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="district">Quận huyện</Label>
          <Input id="district" {...form.register("district")} />
        </div>
        <div className="space-y-2">
          <Label htmlFor="province">Tỉnh thành</Label>
          <Input id="province" {...form.register("provinceCity")} />
        </div>
      </div>
      <div className="space-y-2">
        <Label htmlFor="description">Mô tả</Label>
        <Textarea id="description" {...form.register("description")} />
      </div>
      <Button disabled={isSubmitting}>
        <Plus className="h-4 w-4" />
        Thêm khu trọ
      </Button>
    </form>
  );
}
