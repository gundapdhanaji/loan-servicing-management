package com.loanservicing.auth;

/**
 * PUBLIC API of the auth module - the only way other modules may talk to it.
 *
 * Microservice note: after the split, this interface stays the same and gets a second
 * implementation that calls auth-service over HTTP (e.g. with OpenFeign).
 */
public interface AuthApi {

    /** Creates a login and returns the new user's id. */
    Long createUser(String email, String rawPassword, Role role);

    boolean emailExists(String email);
}
