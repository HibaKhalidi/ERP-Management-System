package com.erp.domain;

import com.erp.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;
    private String contactName;
    @Email
    private String email;
    private String phone;
    private String address;
    private String city;
}
