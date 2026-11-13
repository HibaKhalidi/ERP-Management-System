package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.Specs;
import com.erp.domain.Invoice;
import com.erp.domain.OrderStatus;
import com.erp.domain.SalesOrder;
import com.erp.repository.SalesOrderRepository;
import com.erp.service.InvoiceService;
import com.erp.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Filtres : ?status= &customerId= &employeeId= */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Commandes")
public class OrderController extends BaseCrudController<SalesOrder> {

    private final OrderService orders;
    private final InvoiceService invoices;

    public OrderController(SalesOrderRepository repo, OrderService orders, InvoiceService invoices) {
        super(repo, "Commande");
        this.orders = orders;
        this.invoices = invoices;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("number", "customer.name", "notes");
    }

    @Override
    protected Specification<SalesOrder> filters(Map<String, String> p) {
        return Specs.<SalesOrder>equal("status", Specs.toEnum(OrderStatus.class, p.get("status")))
                .and(Specs.equal("customer.id", Specs.toLong(p.get("customerId"))))
                .and(Specs.equal("employee.id", Specs.toLong(p.get("employeeId"))));
    }

    @Override
    protected SalesOrder doCreate(SalesOrder entity) {
        return orders.create(entity);
    }

    @Override
    protected SalesOrder doUpdate(Long id, SalesOrder incoming) {
        return orders.update(id, incoming);
    }

    @Override
    protected void doDelete(Long id) {
        orders.delete(id);
    }

    /** Changement de statut : CONFIRMED (décrémente le stock), SHIPPED, DELIVERED, CANCELLED (réintègre le stock). */
    @PostMapping("/{id}/status")
    public SalesOrder changeStatus(@PathVariable Long id, @RequestParam OrderStatus value) {
        return orders.changeStatus(id, value);
    }

    /** Génère la facture de la commande (échéance par défaut : +30 jours). */
    @PostMapping("/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice createInvoice(@PathVariable Long id,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return invoices.createFromOrder(id, dueDate);
    }
}
