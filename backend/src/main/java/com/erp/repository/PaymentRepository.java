package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.Payment;


public interface PaymentRepository extends BaseRepository<Payment> {
    java.util.List<Payment> findByInvoiceId(Long invoiceId);
}
