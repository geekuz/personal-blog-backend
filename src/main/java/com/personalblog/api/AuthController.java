package com.personalblog.api;

import com.personalblog.api.dto.AuthDtos.*;
import com.personalblog.user.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
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
    private final BlogUserService users;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public AuthController(BlogUserService users, AuthenticationManager authenticationManager) {
        this.users = users;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getToken()); }

    @PostMapping(value = "/register", consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return response(users.register(request.email(), request.password(), request.displayName()));
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
