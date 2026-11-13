package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.common.NotFoundException;
import com.erp.domain.Invoice;
import com.erp.domain.InvoiceStatus;
import com.erp.domain.Payment;
import com.erp.repository.InvoiceRepository;
import com.erp.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final InvoiceRepository invoices;

    public PaymentService(PaymentRepository payments, InvoiceRepository invoices) {
        this.payments = payments;
        this.invoices = invoices;
    }

    @Transactional
    public Payment create(Payment in) {
        if (in.getInvoice() == null || in.getInvoice().getId() == null) {
            throw new BusinessException("Facture obligatoire");
        }
        Invoice inv = invoices.findById(in.getInvoice().getId())
                .orElseThrow(() -> new NotFoundException("Facture introuvable : " + in.getInvoice().getId()));
        if (inv.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Facture annulée");
        }
        if (in.getAmount() == null || in.getAmount().signum() <= 0) {
            throw new BusinessException("Montant invalide");
        }
        if (in.getAmount().compareTo(inv.getBalance()) > 0) {
            throw new BusinessException("Le paiement dépasse le reste à payer (" + inv.getBalance() + ")");
        }
        Payment p = new Payment();
        p.setInvoice(inv);
        p.setAmount(in.getAmount());
        p.setMethod(in.getMethod());
        p.setReference(in.getReference());
        p.setPaidAt(in.getPaidAt() != null ? in.getPaidAt() : LocalDateTime.now());
        Payment saved = payments.save(p);
        recompute(inv, inv.getPaidAmount().add(saved.getAmount()));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Payment p = payments.findById(id).orElseThrow(() -> new NotFoundException("Paiement introuvable : " + id));
        Invoice inv = p.getInvoice();
        payments.delete(p);
        recompute(inv, inv.getPaidAmount().subtract(p.getAmount()));
    }

    private void recompute(Invoice inv, BigDecimal paid) {
        inv.setPaidAmount(paid);
        if (paid.compareTo(inv.getTotalAmount()) >= 0) {
            inv.setStatus(InvoiceStatus.PAID);
        } else if (paid.signum() > 0) {
            inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            inv.setStatus(InvoiceStatus.UNPAID);
        }
        invoices.save(inv);
    }
}
