package com.personalblog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalblog.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Admin-API-Key";
    private final byte[] configuredKey;
    private final ObjectMapper objectMapper;

    public AdminApiKeyFilter(@Value("${blog.admin.api-key:}") String configuredKey, ObjectMapper objectMapper) {
        this.configuredKey = configuredKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] suppliedKey = request.getHeader(HEADER) == null
            ? new byte[0]
            : request.getHeader(HEADER).getBytes(StandardCharsets.UTF_8);
        if (configuredKey.length == 0 || !MessageDigest.isEqual(configuredKey, suppliedKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiError(
                Instant.now(), 401, "UNAUTHORIZED", "A valid admin API key is required",
                request.getRequestURI(), null, request.getHeader("X-Request-ID")));
            return;
        }
        chain.doFilter(request, response);
    }
}
