package com.erp.domain;

import com.erp.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category extends BaseEntity {
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
}
