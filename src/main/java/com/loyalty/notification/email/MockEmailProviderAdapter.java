package com.loyalty.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockEmailProviderAdapter implements EmailProviderAdapter {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[EMAIL SENT] To: {} | Subject: {} | Body: {}", to, subject, body);
    }
}
