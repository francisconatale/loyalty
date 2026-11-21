package com.loyalty.notification.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final EmailProviderAdapter emailProviderAdapter;

    public void sendEmail(String to, String subject, String body) {
        if (to != null && !to.isBlank()) {
            emailProviderAdapter.sendEmail(to.trim(), subject, body);
        }
    }
}
