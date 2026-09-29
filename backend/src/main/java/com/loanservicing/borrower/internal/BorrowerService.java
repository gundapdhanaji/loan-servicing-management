package com.loanservicing.borrower.internal;

import com.loanservicing.auth.AuthApi;
import com.loanservicing.auth.Role;
import com.loanservicing.borrower.AddBankAccountRequest;
import com.loanservicing.borrower.BankAccountDto;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.borrower.CreateBorrowerRequest;
import com.loanservicing.common.BankDetails;
import com.loanservicing.common.BusinessException;
import com.loanservicing.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BorrowerService implements BorrowerApi {

    private final BorrowerRepository borrowerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuthApi authApi;

    @Override
    @Transactional
    public BorrowerDto create(CreateBorrowerRequest r) {
        Long userId = authApi.createUser(r.email(), r.password(), Role.BORROWER);

        Borrower b = new Borrower();
        b.setUserId(userId);
        b.setFirstName(r.firstName());
        b.setLastName(r.lastName());
        b.setEmail(r.email().toLowerCase());
        b.setPhone(r.phone());
        b.setStreet(r.street());
        b.setCity(r.city());
        b.setState(r.state());
        b.setZipCode(r.zipCode());
        b.setTinType(r.tinType());
        b.setTin(r.tin());
        b.setSendLateNotices(r.sendLateNotices() == null || r.sendLateNotices());
        b.setSendPaymentReceipts(r.sendPaymentReceipts() == null || r.sendPaymentReceipts());
        return toDto(borrowerRepository.save(b));
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowerDto get(Long borrowerId) {
        return toDto(find(borrowerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowerDto> findAll() {
        return borrowerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BorrowerDto> findByUserId(Long userId) {
        return borrowerRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    @Transactional
    public BankAccountDto addBankAccount(Long borrowerId, AddBankAccountRequest r) {
        Borrower borrower = find(borrowerId);
        BankAccount account = new BankAccount();
        account.setBankName(r.bankName());
        account.setAccountHolderName(r.accountHolderName());
        account.setRoutingNumber(r.routingNumber());
        account.setAccountNumber(r.accountNumber());
        account.setAccountType(r.accountType());
        borrower.addBankAccount(account);
        return toDto(bankAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountDto> getBankAccounts(Long borrowerId) {
        return bankAccountRepository.findByBorrowerIdAndActiveTrue(borrowerId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BankDetails getBankDetails(Long borrowerId, Long bankAccountId) {
        BankAccount account = findAccount(borrowerId, bankAccountId);
        if (!account.isActive()) {
            throw new BusinessException("This bank account has been removed");
        }
        return new BankDetails(account.getAccountHolderName(), account.getRoutingNumber(), account.getAccountNumber());
    }

    @Transactional
    public void removeBankAccount(Long borrowerId, Long bankAccountId) {
        findAccount(borrowerId, bankAccountId).setActive(false);
    }

    private BankAccount findAccount(Long borrowerId, Long bankAccountId) {
        // Looking up by id AND borrowerId means a borrower can never use someone else's account.
        return bankAccountRepository.findByIdAndBorrowerId(bankAccountId, borrowerId)
                .orElseThrow(() -> NotFoundException.of("Bank account", bankAccountId));
    }

    private Borrower find(Long id) {
        return borrowerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Borrower", id));
    }

    private BorrowerDto toDto(Borrower b) {
        return new BorrowerDto(b.getId(), b.getUserId(), b.getFirstName(), b.getLastName(), b.getFullName(),
                b.getEmail(), b.getPhone(), b.getStreet(), b.getCity(), b.getState(), b.getZipCode(),
                b.getTinType(), BankDetails.mask(b.getTin()), b.isSendLateNotices(), b.isSendPaymentReceipts(),
                b.getCreatedAt());
    }

    private BankAccountDto toDto(BankAccount a) {
        return new BankAccountDto(a.getId(), a.getBankName(), a.getAccountHolderName(), a.getRoutingNumber(),
                BankDetails.mask(a.getAccountNumber()), a.getAccountType());
    }
}
