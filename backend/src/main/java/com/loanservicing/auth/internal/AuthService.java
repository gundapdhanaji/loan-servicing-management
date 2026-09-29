package com.loanservicing.auth.internal;

import com.loanservicing.auth.AuthApi;
import com.loanservicing.auth.Role;
import com.loanservicing.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService implements AuthApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public Long createUser(String email, String rawPassword, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("A user with email " + email + " already exists");
        }
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * LoanLinq-style login: POST /api/v1/auth/get_auth_token {username, password}.
     * "username" is the email address, like in LoanLinq.
     */
    @Transactional(readOnly = true)
    public AuthTokenResponse issueToken(AuthTokenRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.username())
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Same message for "no such user" and "wrong password" - don't reveal which emails exist
                .orElseThrow(() -> new BadCredentialsException("Invalid email and/or password"));

        String token = jwtService.createToken(user);
        // is_trusted = true: no OTP step locally (LoanLinq asks for an OTP on untrusted devices)
        return new AuthTokenResponse(token, user.getId(), user.getEmail(), user.getEmail(),
                user.getRole().name(), jwtService.expirationMillis(), new AuthTokenResponse.AxiosData(true));
    }
}
