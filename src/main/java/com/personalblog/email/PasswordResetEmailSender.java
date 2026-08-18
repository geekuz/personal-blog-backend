package com.personalblog.email;

public interface PasswordResetEmailSender {
    void sendReset(String recipient, String displayName, String resetUrl);
}
