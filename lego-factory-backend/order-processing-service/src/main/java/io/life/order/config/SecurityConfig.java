package io.life.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Allow internal service-to-service calls from SimAL
                .requestMatchers("/api/production-control-orders").permitAll()
                .requestMatchers("/api/production-control-orders/**").permitAll()
                .requestMatchers("/api/assembly-control-orders").permitAll()
                .requestMatchers("/api/assembly-control-orders/**").permitAll()
                .requestMatchers("/api/production-orders/**").permitAll()
                .requestMatchers("/api/warehouse-orders/**").permitAll()
                .requestMatchers("/api/supply-orders/**").permitAll()
                // Workstation-specific order endpoints (WS-1 to WS-5)
                .requestMatchers("/api/injection-molding-orders/**").permitAll()
                .requestMatchers("/api/part-preproduction-orders/**").permitAll()
                .requestMatchers("/api/part-finishing-orders/**").permitAll()
                .requestMatchers("/api/gear-assembly-orders/**").permitAll()
                .requestMatchers("/api/motor-assembly-orders/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }
}
