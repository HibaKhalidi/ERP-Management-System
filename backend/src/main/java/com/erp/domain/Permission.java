package com.erp.domain;

import com.erp.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission extends BaseEntity {
    /** Ex. PRODUCT_READ, PRODUCT_WRITE */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;
}
