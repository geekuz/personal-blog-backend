package com.personalblog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personalblog.config.AuthRateLimitInterceptor;
import com.personalblog.email.NewsletterConfirmationEmailSender;
import com.personalblog.newsletter.NewsletterSubscription;
import com.personalblog.newsletter.NewsletterSubscriptionRepository;
import com.personalblog.newsletter.NewsletterTokenService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:reset-blog-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PublicNewsletterApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired NewsletterSubscriptionRepository subscriptions;
    @Autowired NewsletterTokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthRateLimitInterceptor rateLimits;
    @MockitoBean NewsletterConfirmationEmailSender sender;

    @BeforeEach void resetRateLimits() {
        rateLimits.clear();
    }

    @Test void requestRequiresCsrfAndValidEmail() throws Exception {
        mvc.perform(post("/api/v1/newsletter/public/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
            .andExpect(status().isBadRequest());
        verify(sender, never()).sendConfirmation(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test void publicReaderConfirmsAndUnsubscribesWithHashedSingleUseTokens() throws Exception {
        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Reader@Example.COM\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value(
                "If this address can be subscribed, a confirmation email is on its way."));

        ArgumentCaptor<String> url = forClass(String.class);
        verify(sender).sendConfirmation(org.mockito.ArgumentMatchers.eq("reader@example.com"), url.capture());
        String confirmationToken = tokenFrom(url.getValue());
        String storedHash = jdbc.queryForObject(
            "select token_hash from newsletter_subscription_tokens where purpose = 'CONFIRM'", String.class);
        assertThat(storedHash).hasSize(64).doesNotContain(confirmationToken);
        NewsletterSubscription pending = subscriptions.findByEmailIgnoreCase("reader@example.com").orElseThrow();
        assertThat(pending.isConfirmed()).isFalse();

        mvc.perform(post("/api/v1/newsletter/public/confirm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + confirmationToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Your subscription is confirmed."));
        assertThat(subscriptions.findByEmailIgnoreCase("reader@example.com").orElseThrow().isConfirmed()).isTrue();

        mvc.perform(post("/api/v1/newsletter/public/confirm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + confirmationToken + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_NEWSLETTER_TOKEN"));

        String unsubscribeToken = tokens.issueUnsubscribe(
            subscriptions.findByEmailIgnoreCase("reader@example.com").orElseThrow(), Instant.now());
        mvc.perform(post("/api/v1/newsletter/public/unsubscribe").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + unsubscribeToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("You have been unsubscribed."));
        assertThat(subscriptions.findByEmailIgnoreCase("reader@example.com")).isEmpty();
    }

    @Test void repeatedRequestReturnsTheSameGenericResponseWithoutSendingAgain() throws Exception {
        String body = "{\"email\":\"reader@example.com\"}";
        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.message").value(
                "If this address can be subscribed, a confirmation email is on its way."));
        verify(sender).sendConfirmation(org.mockito.ArgumentMatchers.eq("reader@example.com"),
            org.mockito.ArgumentMatchers.any());
        verifyNoMoreInteractions(sender);
    }

    @Test void expiredConfirmationTokenIsRejected() throws Exception {
        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isAccepted());
        ArgumentCaptor<String> url = forClass(String.class);
        verify(sender).sendConfirmation(org.mockito.ArgumentMatchers.eq("reader@example.com"), url.capture());
        jdbc.update("update newsletter_subscription_tokens set expires_at = current_timestamp - interval '1' hour");

        mvc.perform(post("/api/v1/newsletter/public/confirm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + tokenFrom(url.getValue()) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_NEWSLETTER_TOKEN"));
    }

    @Test void signupRequestsAreRateLimited() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"reader@example.com\"}"))
                .andExpect(status().isAccepted());
        }
        mvc.perform(post("/api/v1/newsletter/public/requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("AUTH_RATE_LIMIT"));
    }

    private String tokenFrom(String url) {
        return URLDecoder.decode(url.substring(url.indexOf("token=") + 6), StandardCharsets.UTF_8);
    }
}
