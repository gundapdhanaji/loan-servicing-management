package com.loanservicing.profile.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.loanservicing.auth.AuthenticatedUser;
import com.loanservicing.auth.CurrentUser;
import com.loanservicing.auth.Role;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.lender.LenderApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Second step of the LoanLinq login (the "access_account" call in LoginComp.jsx):
 *
 *   axios.post(access_account.endpoint, { userid: localStorage.getItem("apex_userid") })
 *
 * The React app reads from the response:
 *   res.data.role[].role_name   -> which menus/pages to show
 *   res.data.account[]          -> the lender/borrower accounts in the account selector (recid + type)
 *   res.data.username / email
 *   res.data.demo_yn
 *
 * It lives in its own small "profile" module because it needs auth + borrower + lender together,
 * and auth itself must not depend on borrower/lender (that would be a cycle).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccessAccountController {

    private final BorrowerApi borrowerApi;
    private final LenderApi lenderApi;

    public record AccessAccountRequest(Long userid) {
    }

    public record RoleItem(@JsonProperty("role_name") String roleName) {
    }

    public record AccountItem(Long recid, String type, String name) {
    }

    public record AccessAccountResponse(
            Long userid,
            String username,
            String email,
            List<RoleItem> role,
            List<AccountItem> account,
            @JsonProperty("demo_yn") String demoYn) {
    }

    @PostMapping("/access_account")
    public AccessAccountResponse accessAccount(@RequestBody(required = false) AccessAccountRequest request) {
        AuthenticatedUser user = CurrentUser.get();
        if (request != null && request.userid() != null && !request.userid().equals(user.userId())
                && user.role() != Role.ADMIN) {
            throw new AccessDeniedException("You can only load your own account");
        }

        List<AccountItem> accounts = new ArrayList<>();
        if (user.role() == Role.BORROWER) {
            borrowerApi.findByUserId(user.userId())
                    .ifPresent(b -> accounts.add(new AccountItem(b.id(), "BORROWER", b.fullName())));
        } else if (user.role() == Role.LENDER) {
            lenderApi.findByUserId(user.userId())
                    .ifPresent(l -> accounts.add(new AccountItem(l.id(), "LENDER", l.name())));
        }

        return new AccessAccountResponse(user.userId(), user.email(), user.email(),
                List.of(new RoleItem(user.role().name())), accounts, "N");
    }
}
