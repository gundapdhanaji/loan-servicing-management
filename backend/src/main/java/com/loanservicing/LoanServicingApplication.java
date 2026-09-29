package com.loanservicing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Loan Servicing backend - a MODULAR MONOLITH.
 *
 * Each top-level package below com.loanservicing is one module:
 *   common, auth, lender, borrower, loan, payment, notification, devtools
 *
 * Rules (checked by ModularityTests):
 *   - Classes directly in a module's package (e.g. com.loanservicing.loan.LoanApi) are its PUBLIC API.
 *   - Classes in sub-packages (e.g. com.loanservicing.loan.internal.*) are PRIVATE to that module.
 *   - Modules never share JPA entities; they refer to each other by ID (Long borrowerId, not Borrower).
 * That is what lets each module become its own microservice later.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LoanServicingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServicingApplication.class, args);
    }
}
