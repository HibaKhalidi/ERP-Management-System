package com.erp.domain;

import com.erp.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String number;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", unique = true)
    @JsonIgnoreProperties({"lines"})
    private SalesOrder order;

    @Column(nullable = false)
    private LocalDate issueDate = LocalDate.now();

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    /** Reste à payer (calculé). */
    public BigDecimal getBalance() {
        return totalAmount.subtract(paidAmount);
    }
}
