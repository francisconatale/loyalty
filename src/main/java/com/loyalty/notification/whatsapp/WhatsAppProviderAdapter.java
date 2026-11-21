package com.loyalty.notification.whatsapp;

public interface WhatsAppProviderAdapter {
    void sendMessage(String phone, String templateName, String payload);
}
