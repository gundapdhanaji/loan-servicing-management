package com.loanservicing.auth;

/** The logged-in user, taken from the JWT on every request. */
public record AuthenticatedUser(Long userId, String email, Role role) {
}
