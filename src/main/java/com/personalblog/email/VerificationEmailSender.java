package com.personalblog.email;

public interface VerificationEmailSender {
    void send(String recipient, String displayName, String verificationUrl);
}
