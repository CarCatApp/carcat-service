package com.carland.carland_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * tr: Spring Security yapılandırmasıdır; CSRF/form-login'i kapatır, public path'lere (Swagger, legal,
 *     webhook, admin UI vb.) izin verir, kalan tüm istekleri authenticated yapar ve CustomFilter'ı zincire ekler.
 * en: Spring Security configuration; disables CSRF/form-login, permits public paths (Swagger, legal,
 *     webhook, admin UI etc.), requires authentication for everything else and registers CustomFilter in the chain.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class JWTConfiguration {

    private final CustomFilter customFilter;

    /**
     * tr: SecurityFilterChain bean'ini kurar: public path izinleri, session politikası ve CustomFilter kaydı burada.
     * en: Builds the SecurityFilterChain bean: public path permissions, session policy and CustomFilter registration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/swagger-custom.js",
                                "/swagger-auth-config").permitAll()
                        .requestMatchers("/legal/**").permitAll()
                        .requestMatchers("/api/v1/user/customer-cars").permitAll()
                        .requestMatchers("/api/v1/group/by/get/brand/list/with/models").permitAll()
                        .requestMatchers("/webhook/**").permitAll()
                        .requestMatchers("/admin", "/admin/**", "/admin/").permitAll()
                        .requestMatchers("/api/v1/car/test/**").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
