package com.loanservicing.auth.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Same shape the LoanLinq front-end expects from get_auth_token:
 *   res.data.token              -> stored in localStorage "AUTH-TOKEN"
 *   res.data.userid             -> stored in localStorage "apex_userid"
 *   res.data.axiosdata.is_trusted -> true = skip the OTP screen
 */
public record AuthTokenResponse(
        String token,
        Long userid,
        String username,
        String email,
        String role,
        @JsonProperty("expires_in") long expiresIn,
        AxiosData axiosdata) {

    public record AxiosData(@JsonProperty("is_trusted") boolean isTrusted) {
    }
}
