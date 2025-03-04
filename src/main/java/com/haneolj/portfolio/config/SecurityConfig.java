package com.haneolj.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.ContentTypeOptionsConfig;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 활성화 (폼 제출에 CSRF 토큰 요구)
                .csrf(csrf -> csrf
                        // API 엔드포인트는 CSRF 보호에서 제외 (GitHub 웹훅 등)
                        .ignoringRequestMatchers("/api/webhook/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/study/view/**", "/refresh").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()
                        .requestMatchers("/api/study/graph").permitAll()
                        .anyRequest().authenticated()
                )
                // HTTP 기본 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        // 클릭재킹 방지
                        .frameOptions(FrameOptionsConfig::deny)
                        // XSS 보호
                        .xssProtection(xss -> xss.headerValue(HeaderValue.ENABLED_MODE_BLOCK))
                        // 콘텐츠 타입 스니핑 방지
                        .contentTypeOptions(ContentTypeOptionsConfig::disable)
                        // HSTS 설정 (HTTPS 강제)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)) // 1년
                        // CSP 설정
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; " +
                                        "style-src 'self' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; " +
                                        "font-src 'self' https://cdnjs.cloudflare.com data:; " +  // 폰트 소스 추가
                                        "img-src 'self' data:; " +
                                        "connect-src 'self'; " +
                                        "frame-ancestors 'none';"))
                );

        return http.build();
    }
}