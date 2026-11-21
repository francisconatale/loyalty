package com.loyalty.integration.service;

import com.loyalty.integration.domain.PosIntegration;
import com.loyalty.integration.domain.PosIntegrationRepository;
import com.loyalty.shared.security.ApiKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PosIntegrationService implements ApiKeyResolver {

    private final PosIntegrationRepository posIntegrationRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ResolvedIntegration> resolveApiKey(String apiKey) {
        String hash = hashApiKey(apiKey);
        return posIntegrationRepository.findByApiKeyHash(hash)
                .filter(PosIntegration::isActive)
                .map(i -> new ResolvedIntegration(i.getTenantId(), i.getId(), i.getProvider()));
    }

    public static String hashApiKey(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
