package com.erp.web;

import com.erp.common.BaseCrudController;
import com.erp.common.BusinessException;
import com.erp.common.Specs;
import com.erp.domain.Payment;
import com.erp.domain.PaymentMethod;
import com.erp.repository.PaymentRepository;
import com.erp.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Filtres : ?invoiceId= &method= */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Paiements")
public class PaymentController extends BaseCrudController<Payment> {

    private final PaymentService service;

    public PaymentController(PaymentRepository repo, PaymentService service) {
        super(repo, "Paiement");
        this.service = service;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("reference", "invoice.number");
    }

    @Override
    protected Specification<Payment> filters(Map<String, String> p) {
        return Specs.<Payment>equal("invoice.id", Specs.toLong(p.get("invoiceId")))
                .and(Specs.equal("method", Specs.toEnum(PaymentMethod.class, p.get("method"))));
    }

    @Override
    protected Payment doCreate(Payment in) {
        return service.create(in);
    }

    @Override
    protected Payment doUpdate(Long id, Payment in) {
        throw new BusinessException("Un paiement ne peut pas être modifié : supprimez-le puis recréez-le");
    }

    @Override
    protected void doDelete(Long id) {
        service.delete(id);
    }
}
