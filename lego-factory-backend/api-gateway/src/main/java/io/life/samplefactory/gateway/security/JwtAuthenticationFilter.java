package io.life.samplefactory.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Simple gateway-level JWT validation. Requests to public paths are forwarded without checks,
 * while every other route must include a valid {@code Authorization: Bearer <token>} header.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtParser jwtParser;
    private final List<String> publicPaths = List.of(
        "/api/auth/login",
        "/api/masterdata/product-variants/**",
        "/api/masterdata/modules/**",
        "/api/masterdata/parts/**",
        "/api/production-control-orders",
        "/api/assembly-control-orders",
        "/api/health",
        "/actuator/health",
        "/actuator/info",
        "/error"
    );

    public JwtAuthenticationFilter(GatewayJwtProperties properties) {
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("JWT secret must be configured for the gateway");
        }
        this.jwtParser = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
            .build();
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicPath(path) || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return sendJsonError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            // Use the non-deprecated parseSignedClaims method for jjwt 0.12.x and above
            io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> jws = jwtParser.parseSignedClaims(token);
            Claims claims = jws.getPayload();
            ServerHttpRequest mutated = request.mutate()
                .header("X-Authenticated-User", claims.getSubject())
                .headers(headers -> {
                    Object role = claims.get("role");
                    if (role != null) {
                        headers.set("X-Authenticated-Role", role.toString());
                    }
                    Object userId = claims.get("userId");
                    if (userId != null) {
                        headers.set("X-User-Id", userId.toString());
                    }
                })
                .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException ex) {
            return sendJsonError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    @SuppressWarnings("null")
    private Mono<Void> sendJsonError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"error\":\"" + message + "\",\"status\":" + status.value() + "}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }

    @SuppressWarnings("null")
    private boolean isPublicPath(String path) {
        return publicPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
