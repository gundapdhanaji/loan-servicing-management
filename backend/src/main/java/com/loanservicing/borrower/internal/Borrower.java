package com.loanservicing.borrower.internal;

import com.loanservicing.borrower.TinType;
import com.loanservicing.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowers", schema = "borrower")
@Getter
@Setter
@NoArgsConstructor
public class Borrower extends BaseEntity {

    /** Link to the login in the auth module (by ID, not a JPA relation). */
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    private String street;
    private String city;

    @Column(length = 30)
    private String state;

    @Column(length = 15)
    private String zipCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private TinType tinType;

    @Column(nullable = false, length = 20)
    private String tin;

    private boolean sendLateNotices = true;
    private boolean sendPaymentReceipts = true;

    /** Inside one module, normal JPA relationships are fine. */
    @OneToMany(mappedBy = "borrower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankAccount> bankAccounts = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addBankAccount(BankAccount account) {
        account.setBorrower(this);
        bankAccounts.add(account);
    }
}
