package com.personalblog.email;

public interface NewsletterConfirmationEmailSender {
    void sendConfirmation(String recipient, String confirmationUrl);
}
