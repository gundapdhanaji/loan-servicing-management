package com.loanservicing.borrower.internal;

import com.loanservicing.borrower.AccountType;
import com.loanservicing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A borrower's bank account used for ACH debits (auto-pay). */
@Entity
@Table(name = "borrower_bank_accounts", schema = "borrower")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id")
    private Borrower borrower;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 150)
    private String accountHolderName;

    @Column(nullable = false, length = 9)
    private String routingNumber;

    // Would be encrypted in production.
    @Column(nullable = false, length = 17)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountType accountType;

    /** Soft delete: old payments still point to this account, so we never hard-delete it. */
    private boolean active = true;
}
