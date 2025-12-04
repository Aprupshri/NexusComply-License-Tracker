package com.prodapt.license_tracker_backend.config;

import com.prodapt.license_tracker_backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // User Management - Admin only
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN","SECURITY_HEAD")

                        // Device Management - Admin and Network Admin
                        .requestMatchers("/api/devices/**").hasAnyRole("ADMIN", "NETWORK_ADMIN","OPERATIONS_MANAGER")

                        // License Management - Admin and Network Admin
                        .requestMatchers("/api/licenses/**").hasAnyRole("ADMIN", "NETWORK_ADMIN","PROCUREMENT_OFFICER")
                        // In SecurityConfig.java
                        .requestMatchers("/api/vendors/**").hasAnyRole("ADMIN", "NETWORK_ADMIN","PROCUREMENT_OFFICER")

                        // Dashboard/Reports - All authenticated users
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/alerts/**").hasAnyRole(
                                "ADMIN", "NETWORK_ADMIN", "PROCUREMENT_OFFICER",
                                "COMPLIANCE_OFFICER", "OPERATIONS_MANAGER"
                        )
                        // Add report endpoints
                        .requestMatchers("/api/reports/**").hasAnyRole(
                                "ADMIN", "PROCUREMENT_OFFICER", "COMPLIANCE_OFFICER",
                                "IT_AUDITOR", "SECURITY_HEAD", "COMPLIANCE_LEAD", "PROCUREMENT_LEAD"
                        )
                        .requestMatchers("/api/audit-logs/**").hasAnyRole("ADMIN", "SECURITY_HEAD", "IT_AUDITOR")
                        .requestMatchers("/api/ai/chat/**").hasAnyRole("ADMIN","COMPLIANCE_OFFICER","IT_AUDITOR","COMPLIANCE_LEAD","PROCUREMENT_LEAD","PRODUCT_OWNER")
                        .requestMatchers("/api/software-versions/**").hasAnyRole("ADMIN","OPERATIONS_MANAGER","NETWORK_ENGINEER")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));

         // Allow specific methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));


        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));


        configuration.setAllowCredentials(true);


        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
