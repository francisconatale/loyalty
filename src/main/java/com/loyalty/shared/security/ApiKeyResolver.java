package com.loyalty.shared.security;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyResolver {
    Optional<ResolvedIntegration> resolveApiKey(String apiKey);

    record ResolvedIntegration(UUID tenantId, UUID integrationId, String provider) {}
}
