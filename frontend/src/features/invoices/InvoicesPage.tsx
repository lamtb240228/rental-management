import { useEffect, useMemo, useState } from "react";
import { Ban, CreditCard, Plus, RefreshCw, Trash2, X } from "lucide-react";
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
import { cancelInvoice, createInvoice, createPayment, listInvoices, listPayments, type InvoiceItemPayload, type InvoicePayload, type PaymentPayload } from "./invoiceApi";
import { listContracts } from "../contracts/contractApi";
import { listUtilityReadings } from "../utilities/utilityApi";

const invoiceItemTypes = ["RENT", "ELECTRICITY", "WATER", "SERVICE", "OTHER"] as const;

const emptyItem = (): InvoiceItemPayload => ({
  itemType: "OTHER",
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
  const [items, setItems] = useState<InvoiceItemPayload[]>([]);
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<number | null>(null);
  const [payment, setPayment] = useState<PaymentPayload>({ amount: 0, paymentMethod: "BANK_TRANSFER", transactionReference: "", note: "" });

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
      setItems([]);
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
  const activeContracts = useMemo(
    () => contractsQuery.data?.filter((contract) => contract.status === "ACTIVE") ?? [],
    [contractsQuery.data],
  );

  const selectedContract = activeContracts.find((item) => item.id === contractId);
  const utilityQuery = useQuery({
    queryKey: ["utility-readings", selectedContract?.roomId],
    queryFn: () => listUtilityReadings(selectedContract!.roomId),
    enabled: selectedContract != null,
  });
  const selectedInvoice = invoicesQuery.data?.find((invoice) => invoice.id === selectedInvoiceId) ?? null;
  const paymentsQuery = useQuery({
    queryKey: ["invoices", selectedInvoiceId, "payments"],
    queryFn: () => listPayments(selectedInvoiceId!),
    enabled: selectedInvoiceId != null,
  });

  useEffect(() => {
    if (!selectedContract) return;
    const reading = utilityQuery.data?.find((item) => item.billingYear === billingYear && item.billingMonth === billingMonth);
    const nextItems: InvoiceItemPayload[] = [{ itemType: "RENT", description: "Tiền phòng", quantity: 1, unitPrice: Number(selectedContract.monthlyRent) }];
    if (reading && Number(reading.electricityUsage) > 0) nextItems.push({ itemType: "ELECTRICITY", description: "Tiền điện", quantity: Number(reading.electricityUsage), unitPrice: Number(reading.electricityUnitPrice) });
    if (reading && Number(reading.waterUsage) > 0) nextItems.push({ itemType: "WATER", description: "Tiền nước", quantity: Number(reading.waterUsage), unitPrice: Number(reading.waterUnitPrice) });
    // Rebuild server-derived charges while retaining user-entered services.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setItems((current) => [
      ...nextItems,
      ...current.filter((item) => item.itemType === "SERVICE" || item.itemType === "OTHER"),
    ]);
  }, [billingMonth, billingYear, selectedContract, utilityQuery.data]);

  const paymentMutation = useMutation({
    mutationFn: () => createPayment(selectedInvoiceId!, { ...payment, amount: Number(payment.amount), paymentStatus: "COMPLETED" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["invoices"] });
      queryClient.invalidateQueries({ queryKey: ["invoices", selectedInvoiceId, "payments"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard", "summary"] });
      setPayment({ amount: 0, paymentMethod: "BANK_TRANSFER", transactionReference: "", note: "" });
    },
  });
  const cancelMutation = useMutation({
    mutationFn: cancelInvoice,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["invoices"] }),
  });

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

  function choosePayment(invoice: NonNullable<typeof invoicesQuery.data>[number]) {
    setSelectedInvoiceId(invoice.id);
    setPayment((current) => ({ ...current, amount: Math.max(0, Number(invoice.totalAmount) - Number(invoice.paidAmount)) }));
  }

  function confirmCancelInvoice(id: number, invoiceNumber: string) {
    if (window.confirm(`Hủy hóa đơn ${invoiceNumber}? Hóa đơn đã hủy không thể thu tiền.`)) {
      cancelMutation.mutate(id);
    }
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
                  {activeContracts.map((contract) => (
                    <option key={contract.id} value={contract.id}>
                      {contract.contractCode} — {contract.roomNumber}
                    </option>
                  ))}
                </Select>
                {!contractsQuery.isLoading && activeContracts.length === 0 && (
                  <p className="text-xs text-amber-700">Chỉ hợp đồng đang hoạt động mới có thể lập hóa đơn.</p>
                )}
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
                  <div key={index} className="grid grid-cols-2 gap-3 rounded-lg border border-zinc-200 p-3 sm:p-4">
                    <div className="space-y-2">
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
                    <div className="space-y-2">
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
                        min={0.01}
                        step="0.01"
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
                        <Button type="button" variant="ghost" size="icon" title="Xóa dòng" onClick={() => setItems((current) => current.filter((_, idx) => idx !== index))}>
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
                    <Th>Thao tác</Th>
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
                      <Td><div className="flex gap-1">
                        <Button variant="secondary" size="sm" disabled={invoice.status === "PAID" || invoice.status === "CANCELLED"} onClick={() => choosePayment(invoice)}><CreditCard className="h-4 w-4" />Thu tiền</Button>
                        {Number(invoice.paidAmount) === 0 && invoice.status !== "CANCELLED" && <Button variant="ghost" size="icon" title="Hủy hóa đơn" disabled={cancelMutation.isPending} onClick={() => confirmCancelInvoice(invoice.id, invoice.invoiceNumber)}><Ban className="h-4 w-4" /></Button>}
                      </div></Td>
                    </tr>
                  ))}
                  {invoicesQuery.data?.length === 0 && (
                    <tr>
                      <Td colSpan={7} className="text-zinc-500">
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
                    <div className="col-span-2 flex gap-2">
                      <Button className="flex-1" variant="secondary" disabled={invoice.status === "PAID" || invoice.status === "CANCELLED"} onClick={() => choosePayment(invoice)}><CreditCard className="h-4 w-4" />Thu tiền</Button>
                      {Number(invoice.paidAmount) === 0 && invoice.status !== "CANCELLED" && <Button variant="ghost" size="icon" title="Hủy hóa đơn" disabled={cancelMutation.isPending} onClick={() => confirmCancelInvoice(invoice.id, invoice.invoiceNumber)}><Ban className="h-4 w-4" /></Button>}
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

      {selectedInvoice && (
        <Card>
          <CardHeader className="flex-row items-start justify-between gap-4">
            <div><CardTitle>Thu tiền {selectedInvoice.invoiceNumber}</CardTitle><p className="mt-1 text-sm text-zinc-500">Còn phải thu: {formatCurrency(Number(selectedInvoice.totalAmount) - Number(selectedInvoice.paidAmount))}</p></div>
            <Button variant="ghost" size="icon" title="Đóng" onClick={() => setSelectedInvoiceId(null)}><X className="h-4 w-4" /></Button>
          </CardHeader>
          <CardContent className="grid gap-5 lg:grid-cols-[360px_1fr]">
            <form className="space-y-4" onSubmit={(event) => { event.preventDefault(); paymentMutation.mutate(); }}>
              <div className="space-y-2"><Label htmlFor="payment-amount">Số tiền</Label><Input id="payment-amount" type="number" min={1} max={Number(selectedInvoice.totalAmount) - Number(selectedInvoice.paidAmount)} value={payment.amount} onChange={(e) => setPayment({ ...payment, amount: Number(e.target.value) })} required /></div>
              <div className="space-y-2"><Label htmlFor="payment-method">Phương thức</Label><Select id="payment-method" value={payment.paymentMethod} onChange={(e) => setPayment({ ...payment, paymentMethod: e.target.value as PaymentPayload["paymentMethod"] })}><option value="BANK_TRANSFER">Chuyển khoản</option><option value="CASH">Tiền mặt</option><option value="CARD">Thẻ</option><option value="OTHER">Khác</option></Select></div>
              <div className="space-y-2"><Label htmlFor="payment-reference">Mã giao dịch</Label><Input id="payment-reference" value={payment.transactionReference ?? ""} onChange={(e) => setPayment({ ...payment, transactionReference: e.target.value })} /></div>
              <div className="space-y-2"><Label htmlFor="payment-note">Ghi chú</Label><Textarea id="payment-note" value={payment.note ?? ""} onChange={(e) => setPayment({ ...payment, note: e.target.value })} /></div>
              <Button className="w-full" disabled={paymentMutation.isPending || Number(payment.amount) <= 0}><CreditCard className="h-4 w-4" />{paymentMutation.isPending ? "Đang ghi nhận..." : "Xác nhận thanh toán"}</Button>
              {paymentMutation.isError && <p className="text-sm text-red-600">Không thể ghi nhận. Kiểm tra số tiền hoặc mã giao dịch.</p>}
            </form>
            <div>
              <h3 className="mb-3 text-sm font-semibold text-zinc-900">Lịch sử thanh toán</h3>
              <div className="space-y-2">{(paymentsQuery.data ?? []).map((item) => <div key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-zinc-200 p-3"><div><p className="font-medium">{formatCurrency(Number(item.amount))}</p><p className="text-xs text-zinc-500">{new Date(item.paidAt).toLocaleString("vi-VN")} · {item.paymentMethod}</p></div><Badge>{item.paymentStatus}</Badge></div>)}</div>
              {paymentsQuery.data?.length === 0 && <p className="text-sm text-zinc-500">Chưa có lần thanh toán nào.</p>}
            </div>
          </CardContent>
        </Card>
      )}
      {cancelMutation.isError && <p className="text-sm text-red-600">Không thể hủy hóa đơn đã có thanh toán.</p>}
    </div>
  );
}
