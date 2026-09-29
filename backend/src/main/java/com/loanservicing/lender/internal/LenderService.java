package com.loanservicing.lender.internal;

import com.loanservicing.auth.AuthApi;
import com.loanservicing.auth.Role;
import com.loanservicing.common.BankDetails;
import com.loanservicing.common.NotFoundException;
import com.loanservicing.lender.CreateLenderRequest;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.lender.LenderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LenderService implements LenderApi {

    private final LenderRepository lenderRepository;
    private final AuthApi authApi;

    @Override
    @Transactional
    public LenderDto create(CreateLenderRequest request) {
        Long userId = authApi.createUser(request.email(), request.password(), Role.LENDER);

        Lender lender = new Lender();
        lender.setUserId(userId);
        lender.setName(request.name());
        lender.setEmail(request.email().toLowerCase());
        lender.setPhone(request.phone());
        lender.setTaxId(request.taxId());
        lender.setRoutingNumber(request.routingNumber());
        lender.setAccountNumber(request.accountNumber());
        return toDto(lenderRepository.save(lender));
    }

    @Override
    @Transactional(readOnly = true)
    public LenderDto get(Long lenderId) {
        return toDto(find(lenderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LenderDto> findAll() {
        return lenderRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LenderDto> findByUserId(Long userId) {
        return lenderRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BankDetails getBankDetails(Long lenderId) {
        Lender lender = find(lenderId);
        return new BankDetails(lender.getName(), lender.getRoutingNumber(), lender.getAccountNumber());
    }

    private Lender find(Long id) {
        return lenderRepository.findById(id).orElseThrow(() -> NotFoundException.of("Lender", id));
    }

    private LenderDto toDto(Lender l) {
        return new LenderDto(l.getId(), l.getUserId(), l.getName(), l.getEmail(), l.getPhone(),
                BankDetails.mask(l.getTaxId()), l.getRoutingNumber(), BankDetails.mask(l.getAccountNumber()),
                l.getCreatedAt());
    }
}
