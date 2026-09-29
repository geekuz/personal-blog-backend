package com.personalblog.api;

import com.personalblog.api.dto.NewsletterTokenRequest;
import com.personalblog.api.dto.PublicNewsletterRequest;
import com.personalblog.api.dto.PublicNewsletterResponse;
import com.personalblog.newsletter.PublicNewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/newsletter/public", produces = "application/json;charset=UTF-8")
public class PublicNewsletterController {
    private final PublicNewsletterService newsletter;

    public PublicNewsletterController(PublicNewsletterService newsletter) {
        this.newsletter = newsletter;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublicNewsletterResponse request(@Valid @RequestBody PublicNewsletterRequest request) {
        newsletter.requestSubscription(request.email());
        return new PublicNewsletterResponse(
            "If this address can be subscribed, a confirmation email is on its way.");
    }

    @PostMapping("/confirm")
    public PublicNewsletterResponse confirm(@Valid @RequestBody NewsletterTokenRequest request) {
        newsletter.confirm(request.token());
        return new PublicNewsletterResponse("Your subscription is confirmed.");
    }

    @PostMapping("/unsubscribe")
    public PublicNewsletterResponse unsubscribe(@Valid @RequestBody NewsletterTokenRequest request) {
        newsletter.unsubscribe(request.token());
        return new PublicNewsletterResponse("You have been unsubscribed.");
    }
}
