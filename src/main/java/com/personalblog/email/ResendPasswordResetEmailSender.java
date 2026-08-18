package com.personalblog.email;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class ResendPasswordResetEmailSender implements PasswordResetEmailSender {
    private final RestClient client;
    private final String apiKey;
    private final String from;

    public ResendPasswordResetEmailSender(RestClient.Builder builder,
                                          @Value("${blog.email.resend-api-key:}") String apiKey,
                                          @Value("${blog.email.from:onboarding@resend.dev}") String from) {
        this.client = builder.baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendReset(String recipient, String displayName, String resetUrl) {
        if (apiKey.isBlank()) throw new EmailDeliveryException("Resend is not configured");
        String safeName = HtmlUtils.htmlEscape(displayName);
        String safeUrl = HtmlUtils.htmlEscape(resetUrl);
        String html = """
            <h1>Reset your password</h1>
            <p>Hello %s,</p>
            <p>Use the link below to choose a new password for your otabek.dev account.</p>
            <p><a href="%s">Reset password</a></p>
            <p>This single-use link expires in 30 minutes. If you did not request it, you can ignore this message.</p>
            """.formatted(safeName, safeUrl);
        try {
            client.post().uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("from", from, "to", List.of(recipient),
                    "subject", "Reset your otabek.dev password", "html", html))
                .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new EmailDeliveryException("Password reset email could not be sent", ex);
        }
    }
}
