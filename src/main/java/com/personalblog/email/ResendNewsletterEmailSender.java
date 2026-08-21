package com.personalblog.email;

import com.personalblog.post.Post;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.util.HtmlUtils;

@Component
public class ResendNewsletterEmailSender implements NewsletterEmailSender {
    private final RestClient client;
    private final String apiKey;
    private final String from;
    private final String frontendUrl;

    public ResendNewsletterEmailSender(RestClient.Builder builder,
            @Value("${blog.email.resend-api-key:}") String apiKey,
            @Value("${blog.email.from:onboarding@resend.dev}") String from,
            @Value("${blog.frontend-url:http://localhost:5173}") String frontendUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = builder.baseUrl("https://api.resend.com").requestFactory(requestFactory).build();
        this.apiKey = apiKey; this.from = from; this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override public void send(UUID deliveryId, String recipient, String displayName, Post post) {
        if (apiKey.isBlank()) throw new EmailDeliveryException("Resend is not configured");
        String url = frontendUrl + "/blog/" + post.getSlug();
        String html = """
            <h1>%s</h1><p>Hello %s,</p><p>%s</p>
            <p><a href="%s">Read the article</a></p>
            <p>You received this because you subscribed on otabek.dev. You can unsubscribe from your account.</p>
            """.formatted(HtmlUtils.htmlEscape(post.getTitle()), HtmlUtils.htmlEscape(displayName),
                HtmlUtils.htmlEscape(post.getSummary()), HtmlUtils.htmlEscape(url));
        try {
            client.post().uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("Idempotency-Key", "newsletter/" + deliveryId)
                .body(Map.of("from", from, "to", List.of(recipient), "subject", post.getTitle(), "html", html))
                .retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new EmailDeliveryException("Newsletter email could not be sent", ex);
        }
    }
}
