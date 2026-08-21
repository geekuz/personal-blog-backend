package com.personalblog.email;

import com.personalblog.post.Post;
import java.util.UUID;

public interface NewsletterEmailSender {
    void send(UUID deliveryId, String recipient, String displayName, Post post);
}
