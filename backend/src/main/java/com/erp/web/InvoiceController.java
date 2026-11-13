package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.BusinessException;
import com.erp.common.Specs;
import com.erp.domain.Invoice;
import com.erp.domain.InvoiceStatus;
import com.erp.repository.InvoiceRepository;
import com.erp.service.InvoiceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Filtres : ?status= &orderId= &overdue=true */
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Factures")
public class InvoiceController extends BaseCrudController<Invoice> {

    private final InvoiceService service;

    public InvoiceController(InvoiceRepository repo, InvoiceService service) {
        super(repo, "Facture");
        this.service = service;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("number", "order.number", "order.customer.name");
    }

    @Override
    protected Specification<Invoice> filters(Map<String, String> p) {
        Specification<Invoice> spec = Specs.<Invoice>equal("status", Specs.toEnum(InvoiceStatus.class, p.get("status")))
                .and(Specs.equal("order.id", Specs.toLong(p.get("orderId"))));
        if ("true".equalsIgnoreCase(p.get("overdue"))) {
            spec = spec.and((root, q, cb) -> cb.and(
                    root.get("status").in(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID),
                    cb.lessThan(root.<LocalDate>get("dueDate"), LocalDate.now())));
        }
        return spec;
    }

    /** Corps attendu : {"order": {"id": 1}, "dueDate": "2026-12-31"} */
    @Override
    protected Invoice doCreate(Invoice in) {
        if (in.getOrder() == null || in.getOrder().getId() == null) {
            throw new BusinessException("Commande obligatoire");
        }
        return service.createFromOrder(in.getOrder().getId(), in.getDueDate());
    }

    /** Seule la date d'échéance est modifiable. */
    @Override
    protected Invoice doUpdate(Long id, Invoice in) {
        return service.updateDueDate(id, in.getDueDate());
    }

    @Override
    protected void doDelete(Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/cancel")
    public Invoice cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
