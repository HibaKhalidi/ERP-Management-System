package com.erp.service;

import com.erp.common.BusinessException;
import com.erp.common.NotFoundException;
import com.erp.domain.*;
import com.erp.repository.InvoiceRepository;
import com.erp.repository.PaymentRepository;
import com.erp.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final SalesOrderRepository orders;
    private final PaymentRepository payments;

    public InvoiceService(InvoiceRepository invoices, SalesOrderRepository orders, PaymentRepository payments) {
        this.invoices = invoices;
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional
    public Invoice createFromOrder(Long orderId, LocalDate dueDate) {
        SalesOrder order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande introuvable : " + orderId));
        if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Une facture ne peut être créée que pour une commande confirmée, expédiée ou livrée");
        }
        if (invoices.existsByOrderId(orderId)) {
            throw new BusinessException("Cette commande est déjà facturée");
        }
        Invoice inv = new Invoice();
        inv.setNumber("TMP-" + UUID.randomUUID());
        inv.setOrder(order);
        inv.setIssueDate(LocalDate.now());
        inv.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(30));
        inv.setTotalAmount(order.getTotalAmount());
        Invoice saved = invoices.save(inv);
        saved.setNumber(String.format("INV-%06d", saved.getId()));
        return invoices.save(saved);
    }

    @Transactional
    public Invoice updateDueDate(Long id, LocalDate dueDate) {
        Invoice inv = get(id);
        if (dueDate != null) {
            inv.setDueDate(dueDate);
        }
        return invoices.save(inv);
    }

    @Transactional
    public Invoice cancel(Long id) {
        Invoice inv = get(id);
        if (inv.getPaidAmount().signum() > 0) {
            throw new BusinessException("Impossible d'annuler une facture ayant des paiements");
        }
        inv.setStatus(InvoiceStatus.CANCELLED);
        return invoices.save(inv);
    }

    @Transactional
    public void delete(Long id) {
        Invoice inv = get(id);
        if (!payments.findByInvoiceId(id).isEmpty()) {
            throw new BusinessException("Impossible de supprimer une facture ayant des paiements");
        }
        invoices.delete(inv);
    }

    public Invoice get(Long id) {
        return invoices.findById(id).orElseThrow(() -> new NotFoundException("Facture introuvable : " + id));
    }
}
