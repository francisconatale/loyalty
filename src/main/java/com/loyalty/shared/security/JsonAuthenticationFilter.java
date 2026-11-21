package com.loyalty.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Map;

public class JsonAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(new AntPathRequestMatcher("/api/auth/login", "POST"));
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }

        Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), Map.class);
        String username = credentials.get("username");
        String password = credentials.get("password");

        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username != null ? username.trim() : "", password != null ? password : "");

        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
