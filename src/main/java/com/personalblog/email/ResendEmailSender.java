package com.personalblog.email;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class ResendEmailSender implements VerificationEmailSender {
    private final RestClient client;
    private final String apiKey;
    private final String from;

    public ResendEmailSender(RestClient.Builder builder,
                             @Value("${blog.email.resend-api-key:}") String apiKey,
                             @Value("${blog.email.from:onboarding@resend.dev}") String from) {
        this.client = builder.baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(String recipient, String displayName, String verificationUrl) {
        if (apiKey.isBlank()) throw new EmailDeliveryException("Resend is not configured");
        String safeName = HtmlUtils.htmlEscape(displayName);
        String safeUrl = HtmlUtils.htmlEscape(verificationUrl);
        String html = """
            <h1>Verify your email</h1>
            <p>Hello %s,</p>
            <p>Confirm your email address to activate account features on otabek.dev.</p>
            <p><a href="%s">Verify email address</a></p>
            <p>This link expires in 24 hours. If you did not create this account, ignore this message.</p>
            """.formatted(safeName, safeUrl);
        try {
            client.post().uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("from", from, "to", List.of(recipient),
                    "subject", "Verify your otabek.dev email", "html", html))
                .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new EmailDeliveryException("Verification email could not be sent", ex);
        }
    }
}
