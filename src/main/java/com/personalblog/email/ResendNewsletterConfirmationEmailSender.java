package com.personalblog.email;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class ResendNewsletterConfirmationEmailSender implements NewsletterConfirmationEmailSender {
    private final RestClient client;
    private final String apiKey;
    private final String from;

    public ResendNewsletterConfirmationEmailSender(RestClient.Builder builder,
            @Value("${blog.email.resend-api-key:}") String apiKey,
            @Value("${blog.email.from:onboarding@resend.dev}") String from) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = builder.baseUrl("https://api.resend.com").requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendConfirmation(String recipient, String confirmationUrl) {
        if (apiKey.isBlank()) throw new EmailDeliveryException("Resend is not configured");
        String safeUrl = HtmlUtils.htmlEscape(confirmationUrl);
        String html = """
            <h1>Confirm your subscription</h1>
            <p>Confirm this email address to receive new writing from otabek.dev.</p>
            <p><a href="%s">Confirm newsletter subscription</a></p>
            <p>This link expires in 24 hours. If you did not request it, ignore this message.</p>
            """.formatted(safeUrl);
        try {
            client.post().uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(Map.of("from", from, "to", List.of(recipient),
                    "subject", "Confirm your otabek.dev subscription", "html", html))
                .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new EmailDeliveryException("Newsletter confirmation email could not be sent", ex);
        }
    }
}
