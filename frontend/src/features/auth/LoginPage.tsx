import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { Building2, LogIn } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { useAuth } from "./AuthProvider";
import { login } from "./authApi";

const schema = z.object({
  email: z.email(),
  password: z.string().min(1),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signIn } = useAuth();
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      signIn(response);
      const from = (location.state as { from?: Location } | null)?.from?.pathname ?? "/";
      navigate(from, { replace: true });
    },
  });

  const errorMessage =
    mutation.error instanceof Error ? mutation.error.message : "Đã có lỗi xảy ra. Vui lòng thử lại.";

  return (
    <main className="grid min-h-screen grid-cols-1 bg-transparent lg:grid-cols-[1.15fr_0.85fr]">
      <section className="hidden bg-[radial-gradient(circle_at_top_left,_rgba(45,212,191,0.22),_transparent_32%),linear-gradient(135deg,_#08101f_0%,_#0f172a_100%)] p-10 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="space-y-8">
          <div className="flex items-center gap-3 text-lg font-semibold">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 shadow-lg shadow-teal-500/30">
              <Building2 className="h-6 w-6" />
            </div>
            Rental Management
          </div>
          <div className="max-w-xl rounded-[2rem] bg-white/10 p-8 ring-1 ring-white/10 backdrop-blur-lg">
            <p className="text-sm font-medium uppercase tracking-[0.24em] text-teal-300">Nhà trọ Việt Nam</p>
            <h1 className="mt-4 text-5xl font-semibold tracking-tight text-white sm:text-6xl">
              Quản lý phòng trọ chuyên nghiệp
            </h1>
            <p className="mt-4 text-base leading-7 text-slate-300">
              Quản lý khu trọ, phòng, hợp đồng, hóa đơn và bảo trì với giao diện rõ ràng, dễ dùng và chuẩn responsive.
            </p>
          </div>
        </div>

        <div className="grid gap-4 rounded-[2rem] bg-white/5 p-8 ring-1 ring-white/10 backdrop-blur-sm">
          <FeatureItem label="Theo dõi dễ dàng" description="Xem nhanh trạng thái phòng, hợp đồng và hóa đơn." />
          <FeatureItem label="Quản lý danh sách" description="Thêm khu trọ, phòng và người thuê trực tiếp từ trang quản trị." />
          <FeatureItem label="Giao diện sạch" description="Thiết kế tối ưu cho desktop và mobile, dễ vận hành hàng ngày." />
        </div>
      </section>

      <section className="flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-8">
        <div className="w-full max-w-md">
          <Card className="overflow-hidden border border-zinc-200/70 shadow-[0_24px_80px_-42px_rgba(15,23,42,0.45)]">
            <div className="border-b border-zinc-100 bg-zinc-50 px-6 py-6">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-500 text-white">
                  <Building2 className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-zinc-950">Rental Management</p>
                  <p className="text-sm text-zinc-500">Đăng nhập chủ trọ</p>
                </div>
              </div>
            </div>
            <CardContent>
              <h2 className="text-2xl font-semibold text-zinc-950">Chào mừng trở lại</h2>
              <p className="mt-2 text-sm text-zinc-500">Sử dụng email và mật khẩu để vào quản trị hệ thống nhà trọ.</p>
              <form className="mt-6 space-y-4" onSubmit={form.handleSubmit((values) => mutation.mutate(values))}>
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" autoComplete="email" {...form.register("email")} />
                  {form.formState.errors.email && <p className="text-sm text-red-600">Email không hợp lệ</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Mật khẩu</Label>
                  <Input id="password" type="password" autoComplete="current-password" {...form.register("password")} />
                </div>
                {mutation.isError && <p className="text-sm text-red-600">{errorMessage}</p>}
                <Button className="w-full" disabled={mutation.isPending}>
                  <LogIn className="h-4 w-4" />
                  Đăng nhập
                </Button>
              </form>
              <div className="mt-5 text-center text-sm text-zinc-600">
                <Link className="font-medium text-teal-700 hover:underline" to="/register">
                  Tạo tài khoản chủ trọ mới
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
