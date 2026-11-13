package com.erp.domain;

import com.erp.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
public class StockMovement extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    /** Positive pour IN/OUT ; signée pour ADJUSTMENT. */
    @Column(nullable = false)
    private int quantity;

    private String reason;
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
