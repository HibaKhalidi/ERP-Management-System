package com.erp.domain;

import com.erp.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @Email
    @Column(unique = true)
    private String email;

    private String phone;
    private String position;

    @PositiveOrZero
    private BigDecimal salary;

    private LocalDate hireDate;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /** Manager hiérarchique (auto-référence). */
    @ManyToOne
    @JoinColumn(name = "manager_id")
    @JsonIgnoreProperties({"manager", "department", "user"})
    private Employee manager;

    /** Compte utilisateur associé (optionnel). */
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnoreProperties({"roles"})
    private AppUser user;
}
