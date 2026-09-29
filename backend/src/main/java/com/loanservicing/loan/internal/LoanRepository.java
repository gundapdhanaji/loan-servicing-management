package com.loanservicing.loan.internal;

import com.loanservicing.loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByBorrowerIdOrderByIdAsc(Long borrowerId);

    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    boolean existsByLoanNumber(String loanNumber);

    @Query("select distinct l from Loan l join l.fundings f where f.lenderId = :lenderId order by l.id")
    List<Loan> findFundedByLender(@Param("lenderId") Long lenderId);
}
