package com.loyalty.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Chain 1: Integration Security for POS event ingestion & coupon validation/redemption
     */
    @Bean
    @Order(1)
    public SecurityFilterChain integrationSecurityFilterChain(HttpSecurity http, ApiKeyResolver apiKeyResolver) throws Exception {
        http
            .securityMatcher("/api/v1/events", "/api/v1/coupons/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasAuthority(Roles.ROLE_INTEGRATION_SERVICE)
            )
            .addFilterBefore(new ApiKeyFilter(apiKeyResolver), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * Chain 2: Application Security for REST BFF endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        JsonAuthenticationFilter jsonFilter = new JsonAuthenticationFilter(authenticationManager);
        jsonFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        jsonFilter.setAuthenticationSuccessHandler((request, response, authentication) ->
                response.setStatus(HttpStatus.OK.value())
        );
        jsonFilter.setAuthenticationFailureHandler((request, response, exception) ->
                response.setStatus(HttpStatus.UNAUTHORIZED.value())
        );

        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterAt(jsonFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpStatus.NO_CONTENT.value())
                )
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * Chain 3: Portal Security for Backoffice Web
     */
    @Bean
    @Order(3)
    public SecurityFilterChain portalSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }
}
