package com.forex.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Handles the server-rendered HTML pages (login, register, converter,
 * dashboard, history, charts, alerts) using session-based form login.
 *
 * This is intentionally a SEPARATE filter chain from the existing
 * stateless JWT chain in SecurityConfig, matched only against the page
 * routes below via securityMatcher(...). The /api/v1/** JWT endpoints
 * are untouched and continue to work exactly as before.
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1) // evaluated before the JWT chain in SecurityConfig
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(
                        "/", "/login", "/register", "/logout",
                        "/converter", "/dashboard", "/history/**",
                        "/charts/**", "/alerts/**", "/sparkline/**",
                        "/css/**", "/js/**", "/images/**"
                )
                .csrf(AbstractHttpConfigurer::disable) // simplifies template forms; re-enable + add CSRF tokens later if desired
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/converter", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }
}
