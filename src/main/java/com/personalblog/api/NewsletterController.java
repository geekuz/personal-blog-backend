package com.personalblog.api;

import com.personalblog.api.dto.NewsletterSubscriptionResponse;
import com.personalblog.newsletter.NewsletterSubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/newsletter/subscription", produces = "application/json;charset=UTF-8")
public class NewsletterController {
    private final NewsletterSubscriptionService subscriptions;

    public NewsletterController(NewsletterSubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @GetMapping
    public NewsletterSubscriptionResponse status(Authentication authentication) {
        return new NewsletterSubscriptionResponse(subscriptions.isSubscribed(authentication.getName()));
    }

    @PostMapping
    public NewsletterSubscriptionResponse subscribe(Authentication authentication) {
        subscriptions.subscribe(authentication.getName());
        return new NewsletterSubscriptionResponse(true);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(Authentication authentication) {
        subscriptions.unsubscribe(authentication.getName());
    }
}
