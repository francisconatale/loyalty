package com.loyalty.shared.security;

import java.util.UUID;

public final class TenantSecurityContext {
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantSecurityContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
