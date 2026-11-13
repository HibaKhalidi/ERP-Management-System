export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Ref { id: number }

export interface Named extends Ref { name: string }

export interface Product extends Ref {
  sku: string; name: string; description?: string; price: number; costPrice?: number;
  quantityInStock: number; reorderLevel: number; active: boolean;
  category?: Named; supplier?: Named;
}

export interface OrderLine { id?: number; product: { id: number; name?: string; sku?: string }; quantity: number; unitPrice?: number; lineTotal?: number }

export type OrderStatus = 'DRAFT' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface SalesOrder extends Ref {
  number: string; status: OrderStatus; orderDate: string; totalAmount: number; notes?: string;
  customer: Named; employee?: { id: number; firstName: string; lastName: string }; lines: OrderLine[];
}

export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';

export interface Invoice extends Ref {
  number: string; order: { id: number; number: string; customer: Named };
  issueDate: string; dueDate: string; totalAmount: number; paidAmount: number; balance: number; status: InvoiceStatus;
}

export interface Dashboard {
  products: number; lowStockCount: number; customers: number; suppliers: number; employees: number;
  ordersByStatus: Record<string, number>;
  totalSales: number; totalInvoiced: number; totalPaid: number; totalOutstanding: number; overdueInvoices: number;
  salesByMonth: { month: string; total: number }[];
  recentOrders: SalesOrder[]; lowStockProducts: Product[];
}
