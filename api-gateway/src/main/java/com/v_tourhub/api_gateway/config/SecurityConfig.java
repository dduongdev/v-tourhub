package com.v_tourhub.api_gateway.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalog/services/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalog/destinations/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/users/{userId}/public").permitAll()
                        .pathMatchers("/api/payments/vnpay-return").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));

        return http.build();
    }

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {

        // 1. Tạo đối tượng cấu hình CORS
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Cho phép request từ origin của Angular App
        corsConfig.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));

        // Cho phép tất cả các method (GET, POST, PUT, DELETE, OPTIONS)
        corsConfig.addAllowedMethod("*");

        // Cho phép tất cả các header, bao gồm cả 'Authorization'
        corsConfig.addAllowedHeader("*");

        // Cho phép trình duyệt gửi cookie (nếu sau này bạn dùng session)
        corsConfig.setAllowCredentials(true);

        // 2. Đăng ký cấu hình này cho tất cả các đường dẫn ('/**')
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        // 3. Tạo và trả về CorsWebFilter
        return new CorsWebFilter(source);
    }
}