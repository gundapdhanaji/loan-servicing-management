package com.loanservicing.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Other modules call CurrentUser.get() to know who is making the request.
 * (After the microservice split, each service reads the same JWT, so this keeps working.)
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Not logged in");
        }
        return user;
    }

    public static boolean hasRole(Role role) {
        return get().role() == role;
    }

    /** ADMIN and CSR can see every loan, borrower and lender. */
    public static boolean isStaff() {
        Role role = get().role();
        return role == Role.ADMIN || role == Role.CSR;
    }
}
