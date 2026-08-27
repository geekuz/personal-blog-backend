package com.personalblog.api;

import com.personalblog.post.PostNotFoundException;
import com.personalblog.post.DuplicatePostSlugException;
import com.personalblog.user.EmailAlreadyRegisteredException;
import com.personalblog.user.InvalidCredentialsException;
import com.personalblog.user.InvalidVerificationTokenException;
import com.personalblog.user.VerificationRateLimitException;
import com.personalblog.user.IncorrectCurrentPasswordException;
import com.personalblog.user.InvalidPasswordResetTokenException;
import com.personalblog.email.EmailDeliveryException;
import com.personalblog.config.AuthRateLimitException;
import com.personalblog.newsletter.EmailVerificationRequiredException;
import com.personalblog.comment.CommentForbiddenException;
import com.personalblog.comment.CommentNotFoundException;
import com.personalblog.comment.CommentRateLimitException;
import com.personalblog.comment.CommentEmailVerificationRequiredException;
import com.personalblog.media.MediaUploadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PostNotFoundException.class)
    ResponseEntity<ApiError> notFound(PostNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found", request, null);
    }

    @ExceptionHandler(DuplicatePostSlugException.class)
    ResponseEntity<ApiError> duplicateSlug(DuplicatePostSlugException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "POST_SLUG_EXISTS", ex.getMessage(), request, null);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> duplicateEmail(EmailAlreadyRegisteredException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account already exists for this email", request, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect", request, null);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    ResponseEntity<ApiError> invalidVerificationToken(InvalidVerificationTokenException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
            "Verification link is invalid or expired", request, null);
    }

    @ExceptionHandler(VerificationRateLimitException.class)
    ResponseEntity<ApiError> verificationRateLimit(VerificationRateLimitException ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "VERIFICATION_RATE_LIMIT",
            "Please wait one minute before requesting another email", request, null);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    ResponseEntity<ApiError> invalidPasswordResetToken(InvalidPasswordResetTokenException ex,
                                                        HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_RESET_TOKEN",
            "Password reset link is invalid or expired", request, null);
    }

    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    ResponseEntity<ApiError> incorrectCurrentPassword(IncorrectCurrentPasswordException ex,
                                                       HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INCORRECT_CURRENT_PASSWORD",
            "Current password is incorrect", request, null);
    }

    @ExceptionHandler(AuthRateLimitException.class)
    ResponseEntity<ApiError> authRateLimit(AuthRateLimitException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
            .body(new ApiError(Instant.now(), HttpStatus.TOO_MANY_REQUESTS.value(),
                "AUTH_RATE_LIMIT", "Too many requests. Please try again later.",
                request.getRequestURI(), null, null));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    ResponseEntity<ApiError> emailDelivery(EmailDeliveryException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED",
            "Verification email could not be sent", request, null);
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    ResponseEntity<ApiError> emailVerificationRequired(EmailVerificationRequiredException ex,
                                                        HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED",
            "Verify your email before subscribing to the newsletter", request, null);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    ResponseEntity<ApiError> commentNotFound(CommentNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found", request, null);
    }

    @ExceptionHandler(CommentEmailVerificationRequiredException.class)
    ResponseEntity<ApiError> commentEmailVerificationRequired(CommentEmailVerificationRequiredException ex,
                                                               HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED",
            "Verify your email before commenting", request, null);
    }

    @ExceptionHandler(CommentForbiddenException.class)
    ResponseEntity<ApiError> commentForbidden(CommentForbiddenException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "COMMENT_DELETE_FORBIDDEN",
            "You cannot delete this comment", request, null);
    }

    @ExceptionHandler(CommentRateLimitException.class)
    ResponseEntity<ApiError> commentRateLimit(CommentRateLimitException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
            .body(new ApiError(Instant.now(), HttpStatus.TOO_MANY_REQUESTS.value(),
                "COMMENT_RATE_LIMIT", "Too many comments. Please try again later.",
                request.getRequestURI(), null, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> resourceNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", request, null);
    }

    @ExceptionHandler(MediaUploadException.class)
    ResponseEntity<ApiError> mediaUpload(MediaUploadException ex, HttpServletRequest request) {
        HttpStatus status = ex.isInvalidInput() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        String code = ex.isInvalidInput() ? "INVALID_IMAGE" : "MEDIA_UPLOAD_FAILED";
        return error(status, code, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            fields.put(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
        });
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, fields);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> bodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Request body is not valid JSON", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request,
            Map.of(ex.getName(), "must be a valid " + (ex.getRequiredType() == null ? "value" : ex.getRequiredType().getSimpleName())));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request failure", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request, Map<String, String> fields) {
        String traceId = request.getHeader("X-Request-ID");
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message,
            request.getRequestURI(), fields, traceId));
    }
}
