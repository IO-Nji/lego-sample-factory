package io.life.simal_integration_service.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Only skip authentication for actuator health/info endpoints
        return path.startsWith("/h2-console") 
            || path.equals("/actuator/health")
            || path.equals("/actuator/info")
            || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.debug("JWT Filter processing: {}", path);

        // Trust the headers set by API Gateway
        String username = request.getHeader("X-Authenticated-User");
        String role = request.getHeader("X-Authenticated-Role");
        String authHeader = request.getHeader("Authorization");

        logger.debug("Headers received - User: {}, Role: {}, Auth: {}", 
            username, role, authHeader != null ? "Present" : "Missing");

        // API Gateway validates JWT and adds headers - we just trust them
        if (username != null && role != null && authHeader != null && authHeader.startsWith("Bearer ")) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication set for user: {}, role: {}", username, role);
        } else {
            logger.warn("Missing headers - User: {}, Role: {}, Auth: {}", 
                username, role, authHeader != null);
        }

        filterChain.doFilter(request, response);
    }
}