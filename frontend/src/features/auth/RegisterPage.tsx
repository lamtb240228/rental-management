import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Building2, UserPlus } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { useAuth } from "./AuthProvider";
import { register } from "./authApi";

const schema = z.object({
  fullName: z.string().min(2),
  email: z.email(),
  phone: z.string().optional(),
  password: z.string().min(8),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: "", email: "", phone: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: (response) => {
      signIn(response);
      navigate("/", { replace: true });
    },
  });

  const errorMessage =
    mutation.error instanceof Error ? mutation.error.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";

  return (
    <main className="grid min-h-screen gap-10 bg-[radial-gradient(circle_at_top_left,_rgba(45,212,191,0.18),_transparent_28%),linear-gradient(135deg,_#f8fafc_0%,_#eff6ff_100%)] px-4 py-10 sm:px-6 lg:grid-cols-[0.95fr_0.85fr]">
      <section className="hidden rounded-[2rem] bg-zinc-950/95 p-10 text-white shadow-2xl ring-1 ring-white/10 backdrop-blur lg:block">
        <div className="space-y-8">
          <div className="flex items-center gap-3 text-lg font-semibold">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 shadow-lg shadow-teal-500/30">
              <Building2 className="h-5 w-5" />
            </div>
            Rental Management
          </div>
          <div className="rounded-[2rem] bg-white/5 p-8 ring-1 ring-white/10">
            <p className="text-sm uppercase tracking-[0.24em] text-teal-300">Chủ trọ</p>
            <h1 className="mt-4 text-5xl font-semibold tracking-tight text-white">Bắt đầu quản lý ngay</h1>
            <p className="mt-4 text-sm leading-7 text-slate-300">
              Đăng ký để quản lý phòng trọ, người thuê, hợp đồng và hóa đơn một cách nhanh chóng.
            </p>
          </div>
        </div>
        <div className="grid gap-4 rounded-[2rem] bg-white/5 p-6 ring-1 ring-white/10">
          <FeatureItem label="Giao diện rõ ràng" description="Trang quản trị tối giản, dễ dùng và chuyên nghiệp." />
          <FeatureItem label="Các chức năng đã hỗ trợ" description="Quản lý khu trọ, phòng và người thuê trực tiếp." />
        </div>
      </section>

      <section className="flex items-center justify-center">
        <div className="w-full max-w-lg">
          <Card className="overflow-hidden border border-zinc-200/70 shadow-[0_24px_80px_-42px_rgba(15,23,42,0.45)]">
            <div className="border-b border-zinc-100 bg-zinc-50 px-6 py-6">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 text-white">
                  <Building2 className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-zinc-950">Đăng ký tài khoản chủ trọ</p>
                  <p className="text-sm text-zinc-500">Tạo tài khoản mới để bắt đầu quản lý.</p>
                </div>
              </div>
            </div>
            <CardContent>
              <form className="space-y-4" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
                <div className="space-y-2">
                  <Label htmlFor="fullName">Họ tên</Label>
                  <Input id="fullName" {...form.register("fullName")} />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input id="email" type="email" {...form.register("email")} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="phone">Số điện thoại</Label>
                    <Input id="phone" {...form.register("phone")} />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Mật khẩu</Label>
                  <Input id="password" type="password" {...form.register("password")} />
                </div>
                {mutation.isError && <p className="text-sm text-red-600">{errorMessage}</p>}
                <Button className="w-full" disabled={mutation.isPending}>
                  <UserPlus className="h-4 w-4" />
                  Tạo tài khoản
                </Button>
              </form>
              <div className="mt-4 text-center text-sm text-zinc-600">
                <Link className="font-medium text-teal-700 hover:underline" to="/login">
                  Đã có tài khoản? Đăng nhập
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>
    </main>
  );
}
function FeatureItem({ label, description }: { label: string; description: string }) {
  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
      <p className="text-sm font-semibold text-white">{label}</p>
      <p className="mt-2 text-sm leading-6 text-slate-300">{description}</p>
    </div>
  );
}
