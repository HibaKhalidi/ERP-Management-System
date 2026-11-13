package com.erp.service;

import com.erp.domain.*;
import com.erp.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class DashboardService {

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final EmployeeRepository employees;
    private final SalesOrderRepository orders;
    private final InvoiceRepository invoices;

    public DashboardService(ProductRepository products, CustomerRepository customers, SupplierRepository suppliers,
                            EmployeeRepository employees, SalesOrderRepository orders, InvoiceRepository invoices) {
        this.products = products;
        this.customers = customers;
        this.suppliers = suppliers;
        this.employees = employees;
        this.orders = orders;
        this.invoices = invoices;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("products", products.count());
        m.put("lowStockCount", products.countLowStock());
        m.put("customers", customers.count());
        m.put("suppliers", suppliers.count());
        m.put("employees", employees.count());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s.name(), orders.countByStatus(s));
        }
        m.put("ordersByStatus", byStatus);

        BigDecimal invoiced = invoices.sumInvoiced(InvoiceStatus.CANCELLED);
        BigDecimal paid = invoices.sumPaid(InvoiceStatus.CANCELLED);
        m.put("totalSales", orders.sumSales(OrderStatus.CANCELLED, OrderStatus.DRAFT));
        m.put("totalInvoiced", invoiced);
        m.put("totalPaid", paid);
        m.put("totalOutstanding", invoiced.subtract(paid));
        m.put("overdueInvoices", invoices.countOverdue(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID), LocalDate.now()));

        // Ventes des 6 derniers mois
        YearMonth start = YearMonth.now().minusMonths(5);
        Map<YearMonth, BigDecimal> sales = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            sales.put(start.plusMonths(i), BigDecimal.ZERO);
        }
        for (SalesOrder o : orders.findSince(start.atDay(1), OrderStatus.CANCELLED, OrderStatus.DRAFT)) {
            sales.merge(YearMonth.from(o.getOrderDate()), o.getTotalAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> monthly = new ArrayList<>();
        sales.forEach((ym, v) -> monthly.add(Map.of("month", ym.toString(), "total", v)));
        m.put("salesByMonth", monthly);

        m.put("recentOrders", orders.findTop5ByOrderByIdDesc());
        m.put("lowStockProducts", products.findLowStock(PageRequest.of(0, 5)));
        return m;
    }
}
