import { useMemo, useState } from "react";
import { Plus, RefreshCw, Trash2 } from "lucide-react";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { PageHeader } from "../../components/ui/page-header";
import { Select } from "../../components/ui/select";
import { Table, Td, Th } from "../../components/ui/table";
import { Textarea } from "../../components/ui/textarea";
import { useMutation, useQuery } from "@tanstack/react-query";
import { queryClient } from "../../lib/query-client/queryClient";
import { formatCurrency } from "../../lib/utils";
import { createInvoice, listInvoices, type InvoiceItemPayload, type InvoicePayload } from "./invoiceApi";
import { listContracts } from "../contracts/contractApi";
import type { ContractItem } from "../../lib/api/types";

const invoiceItemTypes = ["RENT", "ELECTRICITY", "WATER", "SERVICE", "OTHER"] as const;

const emptyItem = (): InvoiceItemPayload => ({
  itemType: "RENT",
  description: "",
  quantity: 1,
  unitPrice: 0,
});

export function InvoicesPage() {
  const invoicesQuery = useQuery({ queryKey: ["invoices"], queryFn: listInvoices });
  const contractsQuery = useQuery({ queryKey: ["contracts"], queryFn: listContracts });
  const [contractId, setContractId] = useState<number | null>(null);
  const [billingMonth, setBillingMonth] = useState(new Date().getMonth() + 1);
  const [billingYear, setBillingYear] = useState(new Date().getFullYear());
  const [dueDate, setDueDate] = useState("");
  const [discountAmount, setDiscountAmount] = useState(0);
  const [notes, setNotes] = useState("");
  const [items, setItems] = useState<InvoiceItemPayload[]>([emptyItem()]);

  const createInvoiceMutation = useMutation({
    mutationFn: createInvoice,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invoices"] });
      setContractId(null);
      setBillingMonth(new Date().getMonth() + 1);
      setBillingYear(new Date().getFullYear());
      setDueDate("");
      setDiscountAmount(0);
      setNotes("");
      setItems([emptyItem()]);
    },
  });

  const totalAmount = useMemo(
    () => items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0) - discountAmount,
    [discountAmount, items],
  );

  const contractMap = useMemo(
    () => Object.fromEntries(contractsQuery.data?.map((item) => [item.id, item.contractCode]) ?? []),
    [contractsQuery.data],
  );

  const selectedContract = contractsQuery.data?.find((item) => item.id === contractId);

  function updateItem(index: number, value: Partial<InvoiceItemPayload>) {
    setItems((current) => current.map((item, idx) => (idx === index ? { ...item, ...value } : item)));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!contractId || !dueDate || !items.length) {
      return;
    }
    createInvoiceMutation.mutate({
      contractId,
      billingYear,
      billingMonth,
      dueDate,
      discountAmount: discountAmount || undefined,
      notes: notes || undefined,
      items,
    } as InvoicePayload);
  }

  return (
    <div className="space-y-6">
      <PageHeader
        subtitle="Quản lý hóa đơn"
        title="Hóa đơn"
        description="Tạo hóa đơn mới và xem lịch sử hóa đơn."
        action={
          <Button variant="secondary" onClick={() => invoicesQuery.refetch()}>
            <RefreshCw className="h-4 w-4" />
            Tải lại
          </Button>
        }
      />

      <div className="grid gap-5 xl:grid-cols-[420px_1fr]">
        <Card className="order-2 xl:order-1">
          <CardHeader>
            <CardTitle>Thêm hóa đơn</CardTitle>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="contract">Hợp đồng</Label>
                <Select
                  id="contract"
                  value={contractId ?? ""}
                  onChange={(event) => setContractId(Number(event.target.value))}
                >
                  <option value="">Chọn hợp đồng</option>
                  {contractsQuery.data?.map((contract) => (
                    <option key={contract.id} value={contract.id}>
                      {contract.contractCode} — {contract.roomNumber}
                    </option>
                  ))}
                </Select>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="billingMonth">Tháng</Label>
                  <Input
                    id="billingMonth"
                    type="number"
                    min={1}
                    max={12}
                    value={billingMonth}
                    onChange={(event) => setBillingMonth(Number(event.target.value))}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="billingYear">Năm</Label>
                  <Input
                    id="billingYear"
                    type="number"
                    min={2000}
                    value={billingYear}
                    onChange={(event) => setBillingYear(Number(event.target.value))}
                    required
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="dueDate">Hạn thanh toán</Label>
                <Input id="dueDate" type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} required />
              </div>

              <div className="space-y-2">
                <Label htmlFor="notes">Ghi chú</Label>
                <Textarea id="notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={4} />
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-medium text-zinc-900">Các khoản</p>
                  <Button type="button" size="sm" variant="secondary" onClick={() => setItems((current) => [...current, emptyItem()])}>
                    <Plus className="h-4 w-4" /> Thêm dòng
                  </Button>
                </div>

                {items.map((item, index) => (
                  <div key={index} className="grid grid-cols-2 gap-3 rounded-2xl border border-zinc-200 p-3 sm:grid-cols-[1fr_0.9fr_0.9fr_0.8fr] sm:p-4">
                    <div className="col-span-2 space-y-2 sm:col-span-1">
                      <Label>Loại</Label>
                      <Select
                        value={item.itemType}
                        onChange={(event) => updateItem(index, { itemType: event.target.value as InvoiceItemPayload["itemType"] })}
                      >
                        {invoiceItemTypes.map((type) => (
                          <option key={type} value={type}>
                            {type}
                          </option>
                        ))}
                      </Select>
                    </div>
                    <div className="col-span-2 space-y-2 sm:col-span-1">
                      <Label>Mô tả</Label>
                      <Input
                        value={item.description}
                        onChange={(event) => updateItem(index, { description: event.target.value })}
                        placeholder="Ví dụ: Tiền phòng"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Số lượng</Label>
                      <Input
                        type="number"
                        min={1}
                        value={item.quantity}
                        onChange={(event) => updateItem(index, { quantity: Number(event.target.value) })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Đơn giá</Label>
                      <div className="flex items-center gap-2">
                        <Input
                          type="number"
                          min={0}
                          value={item.unitPrice}
                          onChange={(event) => updateItem(index, { unitPrice: Number(event.target.value) })}
                        />
                        <Button type="button" variant="ghost" size="icon" onClick={() => setItems((current) => current.filter((_, idx) => idx !== index))}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4">
                <div className="flex items-center justify-between text-sm text-zinc-600">
                  <span>Tổng dự kiến</span>
                  <span>{formatCurrency(totalAmount)}</span>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="discountAmount">Giảm giá</Label>
                  <Input
                    id="discountAmount"
                    type="number"
                    min={0}
                    value={discountAmount}
                    onChange={(event) => setDiscountAmount(Number(event.target.value))}
                  />
                </div>
              </div>

              <Button className="w-full" disabled={createInvoiceMutation.isPending || !contractId || !dueDate || items.length === 0}>
                {createInvoiceMutation.isPending ? "Đang tạo..." : "Tạo hóa đơn"}
              </Button>
              {createInvoiceMutation.isError && (
                <p className="text-sm text-red-600">Không thể tạo hóa đơn. Vui lòng kiểm tra lại thông tin.</p>
              )}
            </form>
          </CardContent>
        </Card>

        <Card className="order-1 xl:order-2">
          <CardHeader>
            <CardTitle>Danh sách hóa đơn</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="hidden overflow-x-auto sm:block">
              <Table>
                <thead>
                  <tr>
                    <Th>Mã hóa đơn</Th>
                    <Th>Hợp đồng</Th>
                    <Th>Kỳ</Th>
                    <Th>Tổng</Th>
                    <Th>Đã trả</Th>
                    <Th>Trạng thái</Th>
                  </tr>
                </thead>
                <tbody>
                  {(invoicesQuery.data ?? []).map((invoice) => (
                    <tr key={invoice.id}>
                      <Td className="font-medium text-zinc-950">{invoice.invoiceNumber}</Td>
                      <Td>{contractMap[invoice.contractId] ?? invoice.contractId}</Td>
                      <Td>{invoice.billingMonth}/{invoice.billingYear}</Td>
                      <Td>{formatCurrency(Number(invoice.totalAmount))}</Td>
                      <Td>{formatCurrency(Number(invoice.paidAmount))}</Td>
                      <Td>
                        <Badge>{invoice.status}</Badge>
                      </Td>
                    </tr>
                  ))}
                  {invoicesQuery.data?.length === 0 && (
                    <tr>
                      <Td colSpan={6} className="text-zinc-500">
                        Chưa có hóa đơn.
                      </Td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </div>
            <div className="space-y-3 sm:hidden">
              {(invoicesQuery.data ?? []).map((invoice) => (
                <div key={invoice.id} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-zinc-950">{invoice.invoiceNumber}</p>
                      <p className="text-sm text-zinc-500">{contractMap[invoice.contractId] ?? invoice.contractId}</p>
                    </div>
                    <Badge>{invoice.status}</Badge>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2">
                    <div className="rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Kỳ</p>
                      <p className="mt-1 text-sm text-zinc-950">{invoice.billingMonth}/{invoice.billingYear}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Tổng</p>
                      <p className="mt-1 text-sm text-zinc-950">{formatCurrency(Number(invoice.totalAmount))}</p>
                    </div>
                    <div className="col-span-2 rounded-xl bg-slate-50 p-3">
                      <p className="text-xs font-medium text-zinc-500">Đã trả</p>
                      <p className="mt-1 text-sm font-semibold text-zinc-950">{formatCurrency(Number(invoice.paidAmount))}</p>
                    </div>
                  </div>
                </div>
              ))}
              {!invoicesQuery.data?.length && (
                <div className="rounded-2xl border border-dashed border-zinc-200 bg-white p-6 text-center text-sm text-zinc-500">
                  Chưa có hóa đơn.
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
