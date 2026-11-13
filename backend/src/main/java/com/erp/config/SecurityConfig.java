package com.erp.config;

import com.erp.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sécurité JWT sans état. Chaque ressource REST est protégée par permission :
 *  GET  /api/{ressource}/**  → {RESSOURCE}_READ
 *  autre méthode             → {RESSOURCE}_WRITE
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** chemin → préfixe de permission */
    public static final Map<String, String> RESOURCES = new LinkedHashMap<>();
    static {
        RESOURCES.put("products", "PRODUCT");
        RESOURCES.put("categories", "CATEGORY");
        RESOURCES.put("suppliers", "SUPPLIER");
        RESOURCES.put("customers", "CUSTOMER");
        RESOURCES.put("departments", "DEPARTMENT");
        RESOURCES.put("employees", "EMPLOYEE");
        RESOURCES.put("stock", "STOCK");
        RESOURCES.put("orders", "ORDER");
        RESOURCES.put("invoices", "INVOICE");
        RESOURCES.put("payments", "PAYMENT");
        RESOURCES.put("users", "USER");
        RESOURCES.put("roles", "USER");
        RESOURCES.put("permissions", "USER");
        RESOURCES.put("dashboard", "DASHBOARD");
    }

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Accès refusé\"}");
                }))
            .authorizeHttpRequests(a -> {
                a.requestMatchers("/api/auth/login", "/actuator/health", "/actuator/info",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                a.requestMatchers("/api/auth/me").authenticated();
                RESOURCES.forEach((path, perm) -> {
                    a.requestMatchers(HttpMethod.GET, "/api/" + path + "/**").hasAuthority(perm + "_READ");
                    a.requestMatchers("/api/" + path + "/**").hasAuthority(perm + "_WRITE");
                });
                a.anyRequest().authenticated();
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
