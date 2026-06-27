export type ApiResponse<T> = {
  data: T;
};

export type UserProfile = {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  roles: string[];
};

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  user: UserProfile;
};

export type DashboardSummary = {
  propertyCount: number;
  availableRoomCount: number;
  occupiedRoomCount: number;
  invoiceCount: number;
  pendingMaintenanceCount: number;
};

export type AdminSummary = {
  userCount: number;
  landlordCount: number;
  tenantAccountCount: number;
  propertyCount: number;
  roomCount: number;
  invoiceCount: number;
  pendingMaintenanceCount: number;
};

export type AdminUser = {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  status: "ACTIVE" | "INACTIVE" | "LOCKED";
  roles: string[];
  lastLoginAt?: string;
  createdAt: string;
};

export type PropertyStatus = "ACTIVE" | "INACTIVE";

export type PropertyItem = {
  id: number;
  name: string;
  addressLine: string;
  ward?: string;
  district?: string;
  provinceCity: string;
  description?: string;
  status: PropertyStatus;
  createdAt: string;
  updatedAt: string;
};

export type RoomStatus = "AVAILABLE" | "OCCUPIED" | "MAINTENANCE" | "INACTIVE";

export type RoomItem = {
  id: number;
  propertyId: number;
  propertyName: string;
  roomNumber: string;
  floorNumber?: number;
  area: number;
  monthlyRent: number;
  defaultDeposit: number;
  maxOccupants: number;
  status: RoomStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
};

export type TenantItem = {
  id: number;
  fullName: string;
  dateOfBirth?: string;
  phone?: string;
  email?: string;
  identityNumber?: string;
  permanentAddress?: string;
  status: "ACTIVE" | "INACTIVE";
  createdAt?: string;
  updatedAt?: string;
};

export type ContractItem = {
  id: number;
  roomId: number;
  roomNumber: string;
  contractCode: string;
  startDate: string;
  endDate?: string;
  monthlyRent: number;
  depositAmount: number;
  status: "DRAFT" | "ACTIVE" | "ENDED" | "CANCELLED";
  tenants: { tenantId: number; fullName: string; primaryTenant: boolean }[];
  notes?: string;
  createdAt: string;
  updatedAt: string;
};

export type InvoiceItem = {
  id: number;
  itemType: string;
  description: string;
  quantity: number;
  unitPrice: number;
  amount: number;
};

export type Invoice = {
  id: number;
  contractId: number;
  invoiceNumber: string;
  billingYear: number;
  billingMonth: number;
  issueDate: string;
  dueDate: string;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  paidAmount: number;
  status: "DRAFT" | "UNPAID" | "PARTIALLY_PAID" | "PAID" | "OVERDUE" | "CANCELLED";
  notes?: string;
  items: InvoiceItem[];
  createdAt: string;
  updatedAt: string;
};

export type Payment = {
  id: number;
  invoiceId: number;
  amount: number;
  paidAt: string;
  paymentMethod: string;
  paymentStatus: string;
  transactionReference?: string;
  note?: string;
  receivedBy?: number;
};

export type UtilityReading = {
  id: number;
  roomId: number;
  billingYear: number;
  billingMonth: number;
  electricityOldReading: number;
  electricityNewReading: number;
  electricityUsage: number;
  electricityUnitPrice: number;
  waterOldReading: number;
  waterNewReading: number;
  waterUsage: number;
  waterUnitPrice: number;
};

export type MaintenanceRequest = {
  id: number;
  roomId: number;
  roomNumber: string;
  tenantId?: number;
  tenantName?: string;
  createdBy: number;
  title: string;
  description: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  submittedAt: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  resolutionNotes?: string;
};

export type TenantPortalSummary = {
  tenant: TenantItem;
  room?: RoomItem;
  activeContract?: ContractItem;
  invoices: Invoice[];
  payments: Payment[];
  utilityReadings: UtilityReading[];
  maintenanceRequests: MaintenanceRequest[];
};
