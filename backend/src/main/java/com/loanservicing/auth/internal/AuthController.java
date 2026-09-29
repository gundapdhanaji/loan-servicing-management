package com.loanservicing.auth.internal;

import com.loanservicing.auth.AuthenticatedUser;
import com.loanservicing.auth.CurrentUser;
import com.loanservicing.common.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login - same URL and body as LoanLinq (urls.admin.getAuthToken):
     *   axios.post("/api/v1/auth/get_auth_token", { username: email, password })
     * The React app then stores:
     *   localStorage "AUTH-TOKEN" = { ...res.data, expiration: Date.now() }
     *   localStorage "apex_userid" = res.data.userid
     */
    @PostMapping("/get_auth_token")
    public AuthTokenResponse getAuthToken(@Valid @RequestBody AuthTokenRequest request) {
        return authService.issueToken(request);
    }

    /** Who am I (from the token)? */
    @GetMapping("/me")
    public AuthenticatedUser me() {
        return CurrentUser.get();
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(Instant.now(), 401,
                "Unauthorized", ex.getMessage(), null));
    }
}
