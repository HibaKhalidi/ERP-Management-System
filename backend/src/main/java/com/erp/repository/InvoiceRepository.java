package com.erp.repository;

import com.erp.common.BaseRepository;
import com.erp.domain.Invoice;
import com.erp.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

public interface InvoiceRepository extends BaseRepository<Invoice> {
    boolean existsByOrderId(Long orderId);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.status <> :cancelled")
    BigDecimal sumInvoiced(@Param("cancelled") InvoiceStatus cancelled);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.status <> :cancelled")
    BigDecimal sumPaid(@Param("cancelled") InvoiceStatus cancelled);

    @Query("select count(i) from Invoice i where i.status in :statuses and i.dueDate < :today")
    long countOverdue(@Param("statuses") Collection<InvoiceStatus> statuses, @Param("today") LocalDate today);
}
