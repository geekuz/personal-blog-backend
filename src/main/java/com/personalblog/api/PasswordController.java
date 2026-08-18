package com.personalblog.api;

import com.personalblog.api.dto.ChangePasswordRequest;
import com.personalblog.api.dto.ForgotPasswordRequest;
import com.personalblog.api.dto.PasswordRecoveryResponse;
import com.personalblog.api.dto.ResetPasswordRequest;
import com.personalblog.user.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = "application/json;charset=UTF-8")
public class PasswordController {
    private static final String RECOVERY_MESSAGE =
        "If an account exists for that email, a password reset link has been sent.";

    private final PasswordService passwords;

    public PasswordController(PasswordService passwords) {
        this.passwords = passwords;
    }

    @PostMapping(value = "/password/forgot", consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PasswordRecoveryResponse forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwords.requestReset(request.email());
        return new PasswordRecoveryResponse(RECOVERY_MESSAGE);
    }

    @PostMapping(value = "/password/reset", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request,
                      HttpServletRequest servletRequest) {
        passwords.reset(request.token(), request.newPassword());
        clearBrowserSession(servletRequest);
    }

    @PostMapping(value = "/password/change", consumes = "application/json")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication,
                       HttpServletRequest servletRequest) {
        passwords.change(authentication.getName(), request.currentPassword(), request.newPassword());
        clearBrowserSession(servletRequest);
    }

    private void clearBrowserSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // The after-commit session invalidator may already have removed it.
            }
        }
        SecurityContextHolder.clearContext();
    }
}
