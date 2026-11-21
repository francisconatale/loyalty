package com.loyalty.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-KEY";
    private final ApiKeyResolver apiKeyResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (apiKey != null && !apiKey.isBlank()) {
            apiKeyResolver.resolveApiKey(apiKey).ifPresent(resolved -> {
                TenantSecurityContext.setTenantId(resolved.tenantId());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        resolved.integrationId().toString(),
                        apiKey,
                        List.of(new SimpleGrantedAuthority(Roles.ROLE_INTEGRATION_SERVICE))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantSecurityContext.clear();
        }
    }
}
