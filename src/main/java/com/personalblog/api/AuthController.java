package com.personalblog.api;

import com.personalblog.api.dto.CsrfResponse;
import com.personalblog.api.dto.LoginRequest;
import com.personalblog.api.dto.RegisterRequest;
import com.personalblog.api.dto.SessionResponse;
import com.personalblog.api.dto.UserResponse;
import com.personalblog.api.dto.VerifyEmailRequest;
import com.personalblog.email.EmailDeliveryException;
import com.personalblog.user.BlogUser;
import com.personalblog.user.BlogUserService;
import com.personalblog.user.EmailVerificationService;
import com.personalblog.user.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = "application/json;charset=UTF-8")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final BlogUserService users;
    private final EmailVerificationService verification;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public AuthController(BlogUserService users, EmailVerificationService verification,
                          AuthenticationManager authenticationManager) {
        this.users = users;
        this.verification = verification;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }

    @PostMapping(value = "/register", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        BlogUser user = users.register(request.email(), request.password(), request.displayName());
        try {
            verification.issue(user);
        } catch (EmailDeliveryException ex) {
            log.warn("Account created but verification email delivery failed for {}", user.getEmail(), ex);
        }
        return response(user);
    }

    @PostMapping(value = "/verify-email", consumes = "application/json")
    public UserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return response(verification.verify(request.token()));
    }

    @PostMapping("/verification/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(Authentication authentication) {
        verification.issue(users.byEmail(authentication.getName()));
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                              HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(Locale.ROOT), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            if (servletRequest.getSession(false) == null) servletRequest.getSession(true);
            else servletRequest.changeSessionId();
            contexts.saveContext(context, servletRequest, servletResponse);
            return response(users.byEmail(authentication.getName()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }

    @GetMapping("/me")
    public SessionResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new SessionResponse(false, null);
        }
        return new SessionResponse(true, response(users.byEmail(authentication.getName())));
    }

    private UserResponse response(BlogUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
            user.isEmailVerified(), user.getRoles());
    }
}
