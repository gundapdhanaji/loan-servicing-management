package com.loanservicing.auth.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * Same body the LoanLinq login page sends:
 *   axios.post(urls.admin.getAuthToken, { username, password, fingerprint, userId })
 * fingerprint and userId are accepted but not used yet (trusted-device check).
 */
public record AuthTokenRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fingerprint,
        String userId) {
}
