package com.personalblog;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalblog.config.AuthRateLimitInterceptor;
import com.personalblog.email.PasswordResetEmailSender;
import com.personalblog.email.VerificationEmailSender;
import com.personalblog.email.EmailDeliveryException;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PasswordRecoveryApiIntegrationTest {
    private static final String EMAIL = "reader@example.com";
    private static final String OLD_PASSWORD = "a-secure-password";
    private static final String NEW_PASSWORD = "a-new-secure-password";
    private static final String REGISTER = """
        {"email":"reader@example.com","password":"a-secure-password","displayName":"Reader One"}
        """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthRateLimitInterceptor rateLimits;
    @MockitoBean VerificationEmailSender verificationEmailSender;
    @MockitoBean PasswordResetEmailSender resetEmailSender;

    @BeforeEach
    void clearRateLimits() {
        rateLimits.clear();
    }

    @Test
    void forgotPasswordDoesNotEnumerateAccountsAndStoresOnlyTokenHash() throws Exception {
        register();

        String knownResponse = mvc.perform(post("/api/v1/auth/password/forgot").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value(
                "If an account exists for that email, a password reset link has been sent."))
            .andReturn().getResponse().getContentAsString();
        String missingResponse = mvc.perform(post("/api/v1/auth/password/forgot").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\"}"))
            .andExpect(status().isAccepted())
            .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(missingResponse).isEqualTo(knownResponse);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(resetEmailSender).sendReset(eq(EMAIL), eq("Reader One"), url.capture());
        String token = tokenFrom(url.getValue());
        Integer rawTokenRows = jdbc.queryForObject(
            "select count(*) from password_reset_tokens where token_hash = ?", Integer.class, token);
        String storedHash = jdbc.queryForObject(
            "select token_hash from password_reset_tokens", String.class);
        org.assertj.core.api.Assertions.assertThat(rawTokenRows).isZero();
        org.assertj.core.api.Assertions.assertThat(storedHash).hasSize(64).doesNotContain(token);
    }

    @Test
    void recoveryDoesNotExposeDeliveryFailureAndDiscardsUndeliveredToken() throws Exception {
        register();
        doThrow(new EmailDeliveryException("test delivery failure")).when(resetEmailSender)
            .sendReset(eq(EMAIL), eq("Reader One"), org.mockito.ArgumentMatchers.anyString());

        mvc.perform(post("/api/v1/auth/password/forgot").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value(
                "If an account exists for that email, a password reset link has been sent."));
        Integer tokens = jdbc.queryForObject("select count(*) from password_reset_tokens", Integer.class);
        org.assertj.core.api.Assertions.assertThat(tokens).isZero();
    }

    @Test
    void expiredResetLinkUsesTheSameSafeErrorAsAnInvalidLink() throws Exception {
        register();
        requestReset();
        String token = capturedResetToken();
        jdbc.update("update password_reset_tokens set expires_at = current_timestamp - interval '1' hour");

        mvc.perform(post("/api/v1/auth/password/reset").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"))
            .andExpect(jsonPath("$.message").value("Password reset link is invalid or expired"));
    }

    @Test
    void resetIsSingleUseChangesPasswordAndInvalidatesExistingSessions() throws Exception {
        register();
        Cookie oldSession = login(OLD_PASSWORD).getResponse().getCookie("BLOG_SESSION");
        requestReset();
        String token = capturedResetToken();

        mvc.perform(post("/api/v1/auth/password/reset").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/me").cookie(oldSession))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false));
        loginExpecting(OLD_PASSWORD, 401);
        loginExpecting(NEW_PASSWORD, 200);
        mvc.perform(post("/api/v1/auth/password/reset").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"another-secure-password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_TOKEN"))
            .andExpect(jsonPath("$.message").value("Password reset link is invalid or expired"));
    }

    @Test
    void authenticatedPasswordChangeChecksCurrentPasswordAndLogsOutEverySession() throws Exception {
        register();
        Cookie sessionOne = login(OLD_PASSWORD).getResponse().getCookie("BLOG_SESSION");
        Cookie sessionTwo = login(OLD_PASSWORD).getResponse().getCookie("BLOG_SESSION");

        mvc.perform(post("/api/v1/auth/password/change").cookie(sessionOne).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INCORRECT_CURRENT_PASSWORD"));

        mvc.perform(post("/api/v1/auth/password/change").cookie(sessionOne).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("BLOG_SESSION", 0));

        mvc.perform(get("/api/v1/auth/me").cookie(sessionOne))
            .andExpect(jsonPath("$.authenticated").value(false));
        mvc.perform(get("/api/v1/auth/me").cookie(sessionTwo))
            .andExpect(jsonPath("$.authenticated").value(false));
        loginExpecting(NEW_PASSWORD, 200);
    }

    @Test
    void passwordEndpointsPreserveAuthenticationAndCsrfBoundaries() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/password/change").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"a-secure-password\",\"newPassword\":\"a-new-secure-password\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginRateLimitReturnsGeneric429AndRetryAfter() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            mvc.perform(post("/api/v1/auth/login").with(csrf())
                    .with(request -> { request.setRemoteAddr("203.0.113.8"); return request; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .with(request -> { request.setRemoteAddr("203.0.113.8"); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMIT"));
    }

    private void register() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(REGISTER)).andExpect(status().isCreated());
    }

    private MvcResult login(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
    }

    private void loginExpecting(String password, int statusCode) throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().is(statusCode));
    }

    private void requestReset() throws Exception {
        mvc.perform(post("/api/v1/auth/password/forgot").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + EMAIL + "\"}"))
            .andExpect(status().isAccepted());
    }

    private String capturedResetToken() {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(resetEmailSender).sendReset(eq(EMAIL), eq("Reader One"), url.capture());
        return tokenFrom(url.getValue());
    }

    private String tokenFrom(String url) {
        return URLDecoder.decode(url.substring(url.indexOf("token=") + 6), StandardCharsets.UTF_8);
    }
}
