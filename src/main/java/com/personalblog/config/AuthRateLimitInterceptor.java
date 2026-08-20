package com.personalblog.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.personalblog.comment.CommentRateLimitException;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private static final Map<String, Limit> LIMITS = Map.of(
        "/api/v1/auth/register", new Limit(5, Duration.ofHours(1)),
        "/api/v1/auth/login", new Limit(10, Duration.ofMinutes(1)),
        "/api/v1/auth/password/forgot", new Limit(5, Duration.ofMinutes(15)),
        "/api/v1/auth/password/reset", new Limit(10, Duration.ofMinutes(15)),
        "/api/v1/auth/password/change", new Limit(5, Duration.ofMinutes(15)),
        "/api/v1/auth/verify-email", new Limit(20, Duration.ofMinutes(15)),
        "/api/v1/auth/verification/resend", new Limit(5, Duration.ofMinutes(15))
    );

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(Duration.ofHours(2))
        .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) return true;
        boolean commentCreation = request.getRequestURI().matches("/api/v1/posts/[^/]+/comments");
        Limit limit = commentCreation
            ? new Limit(10, Duration.ofMinutes(1))
            : LIMITS.get(request.getRequestURI());
        if (limit == null) return true;

        String discriminator = commentCreation && request.getUserPrincipal() != null
            ? request.getUserPrincipal().getName()
            : request.getRemoteAddr();
        String operation = commentCreation ? "/api/v1/comments:create" : request.getRequestURI();
        String key = operation + ':' + discriminator;
        Bucket bucket = buckets.get(key, ignored -> Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(limit.capacity())
                .refillIntervally(limit.capacity(), limit.period()).build())
            .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            if (commentCreation) throw new CommentRateLimitException(retryAfter);
            throw new AuthRateLimitException(retryAfter);
        }
        return true;
    }

    public void clear() { buckets.invalidateAll(); }

    private record Limit(long capacity, Duration period) {}
}
