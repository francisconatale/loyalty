package com.loyalty.notification.email;

public interface EmailProviderAdapter {
    void sendEmail(String to, String subject, String body);
}
