package com.loanservicing.lender.internal;

import com.loanservicing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lenders", schema = "lender")
@Getter
@Setter
@NoArgsConstructor
public class Lender extends BaseEntity {

    /** Link to the login in the auth module - by ID only, never a JPA relationship across modules. */
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 20)
    private String taxId;

    // In production these would be encrypted at rest. Kept plain for local learning.
    @Column(nullable = false, length = 9)
    private String routingNumber;

    @Column(nullable = false, length = 17)
    private String accountNumber;
}
