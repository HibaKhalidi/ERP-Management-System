package com.erp.domain;

import com.erp.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
@Getter
@Setter
public class OrderLine extends BaseEntity {
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private SalesOrder order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"category", "supplier", "description"})
    private Product product;

    @Min(1)
    @Column(nullable = false)
    private int quantity;

    /** Si absent, le prix catalogue du produit est utilisé. */
    private BigDecimal unitPrice;

    private BigDecimal lineTotal;
}
