package io.life.samplefactory.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * 
 * Tests JWT validation, public path bypassing, and header propagation.
 */
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hs256";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        GatewayJwtProperties properties = new GatewayJwtProperties();
        properties.setSecret(TEST_SECRET);
        
        jwtAuthenticationFilter = new JwtAuthenticationFilter(properties);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    private String createValidToken(String username, String role, Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createExpiredToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now.minusSeconds(7200)))
                .setExpiration(Date.from(now.minusSeconds(3600)))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // ==================== Public Path Tests ====================
    
    @Nested
    @DisplayName("Public Path Handling")
    class PublicPathTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/auth/login",
            "/api/masterdata/products",
            "/api/masterdata/products/1",
            "/api/masterdata/modules/5",
            "/api/health",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api/docs/user/swagger-ui.html"
        })
        @DisplayName("Should allow public paths without authentication")
        void shouldAllowPublicPathsWithoutAuth(String path) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get(path)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should allow OPTIONS requests without authentication")
        void shouldAllowOptionsWithoutAuth() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .options("/api/customer-orders")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(exchange);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/production-control-orders",
            "/api/production-control-orders/123",
            "/api/assembly-control-orders",
            "/api/injection-molding-orders/1/start",
            "/api/gear-assembly-orders/complete"
        })
        @DisplayName("Should allow workstation order endpoints without authentication")
        void shouldAllowWorkstationOrderEndpoints(String path) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get(path)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(exchange);
        }
    }

    // ==================== Protected Path Tests ====================
    
    @Nested
    @DisplayName("Protected Path Authentication")
    class ProtectedPathTests {

        @Test
        @DisplayName("Should reject protected path without auth header")
        void shouldRejectProtectedPathWithoutAuthHeader() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Should reject protected path with invalid auth header format")
        void shouldRejectInvalidAuthHeaderFormat() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject protected path with empty Bearer token")
        void shouldRejectEmptyBearerToken() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject protected path with invalid JWT token")
        void shouldRejectInvalidJwtToken() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject protected path with expired JWT token")
        void shouldRejectExpiredJwtToken() {
            String expiredToken = createExpiredToken("testuser");
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject token signed with wrong secret")
        void shouldRejectTokenWithWrongSecret() {
            // Create token with different secret
            SecretKey wrongKey = Keys.hmacShaKeyFor("different-secret-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
            String wrongToken = Jwts.builder()
                    .setSubject("testuser")
                    .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                    .signWith(wrongKey, SignatureAlgorithm.HS256)
                    .compact();
            
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ==================== Valid Token Tests ====================
    
    @Nested
    @DisplayName("Valid Token Processing")
    class ValidTokenTests {

        @Test
        @DisplayName("Should allow protected path with valid JWT token")
        void shouldAllowProtectedPathWithValidToken() {
            String validToken = createValidToken("admin_user", "ADMIN", 1L);
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            // Should call filter chain (request allowed)
            verify(filterChain).filter(any(ServerWebExchange.class));
        }

        @Test
        @DisplayName("Should propagate user info headers from valid token")
        void shouldPropagateUserInfoHeaders() {
            String validToken = createValidToken("warehouse_operator", "PLANT_WAREHOUSE", 42L);
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            jwtAuthenticationFilter.filter(exchange, filterChain).block();
            
            // Verify filter chain was called with mutated request
            verify(filterChain).filter(argThat(ex -> {
                ServerWebExchange mutatedExchange = (ServerWebExchange) ex;
                String authUser = mutatedExchange.getRequest().getHeaders().getFirst("X-Authenticated-User");
                String authRole = mutatedExchange.getRequest().getHeaders().getFirst("X-Authenticated-Role");
                String userId = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id");
                
                return "warehouse_operator".equals(authUser) &&
                       "PLANT_WAREHOUSE".equals(authRole) &&
                       "42".equals(userId);
            }));
        }

        @Test
        @DisplayName("Should handle token without optional claims")
        void shouldHandleTokenWithoutOptionalClaims() {
            // Create token with only required claims
            String basicToken = Jwts.builder()
                    .setSubject("basic_user")
                    .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
                    .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                    .compact();
            
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/customer-orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + basicToken)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(any(ServerWebExchange.class));
        }
    }

    // ==================== Filter Order Tests ====================
    
    @Nested
    @DisplayName("Filter Order")
    class FilterOrderTests {

        @Test
        @DisplayName("Should have highest priority order (-1)")
        void shouldHaveHighestPriorityOrder() {
            assertThat(jwtAuthenticationFilter.getOrder()).isEqualTo(-1);
        }
    }

    // ==================== Edge Cases ====================
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle path with query parameters")
        void shouldHandlePathWithQueryParameters() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/masterdata/products?page=1&size=10")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should handle nested public paths")
        void shouldHandleNestedPublicPaths() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/masterdata/products/1/modules/2/parts")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            // This path matches /api/masterdata/products/**
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            verify(filterChain).filter(exchange);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/api/customer-orders",
            "/api/warehouse-orders",
            "/api/final-assembly-orders",
            "/api/users",
            "/api/inventory"
        })
        @DisplayName("Should require authentication for protected endpoints")
        void shouldRequireAuthForProtectedEndpoints(String path) {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get(path)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();
            
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ==================== Configuration Tests ====================
    
    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("Should throw exception when JWT secret is not configured")
        void shouldThrowWhenSecretNotConfigured() {
            GatewayJwtProperties emptyProperties = new GatewayJwtProperties();
            emptyProperties.setSecret("");
            
            assertThatThrownBy(() -> new JwtAuthenticationFilter(emptyProperties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret must be configured");
        }

        @Test
        @DisplayName("Should throw exception when JWT secret is null")
        void shouldThrowWhenSecretIsNull() {
            GatewayJwtProperties nullProperties = new GatewayJwtProperties();
            // secret is null by default
            
            assertThatThrownBy(() -> new JwtAuthenticationFilter(nullProperties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret must be configured");
        }
    }
}
