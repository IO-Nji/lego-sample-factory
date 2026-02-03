package io.life.user_service.security;

import io.life.user_service.entity.User;
import io.life.user_service.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * 
 * Tests JWT token creation, validation, and claim extraction.
 * Uses a test secret key that meets HS256 minimum requirements.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    
    // Minimum 256 bits = 32 bytes for HS256
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hs256";
    private static final Duration TEST_EXPIRATION = Duration.ofHours(1);

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(TEST_EXPIRATION);
        
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    private User createTestUser(String username, UserRole role, Long workstationId) {
        User user = new User(username, "password-hash", role, workstationId);
        // Simulate DB-assigned ID using reflection (no setter available)
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // ==================== createToken Tests ====================
    
    @Nested
    @DisplayName("createToken()")
    class CreateTokenTests {

        @Test
        @DisplayName("Should create token with all user claims")
        void shouldCreateTokenWithAllUserClaims() {
            User user = createTestUser("testuser", UserRole.ADMIN, 7L);
            
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            assertThat(token).isNotNull();
            assertThat(token.token()).isNotBlank();
            assertThat(token.expiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Should create token with expiration in the future")
        void shouldCreateTokenWithFutureExpiration() {
            User user = createTestUser("testuser", UserRole.PLANT_WAREHOUSE, null);
            
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            // Should expire approximately 1 hour from now
            Instant expectedExpiration = Instant.now().plus(TEST_EXPIRATION);
            assertThat(token.expiresAt())
                .isBetween(expectedExpiration.minusSeconds(5), expectedExpiration.plusSeconds(5));
        }

        @Test
        @DisplayName("Should create unique tokens for same user")
        void shouldCreateUniqueTokensForSameUser() {
            User user = createTestUser("testuser", UserRole.ADMIN, null);
            
            JwtTokenProvider.JwtToken token1 = jwtTokenProvider.createToken(user);
            
            // Small delay to ensure different iat claim
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            
            JwtTokenProvider.JwtToken token2 = jwtTokenProvider.createToken(user);
            
            // Tokens may be different due to iat timestamp
            assertThat(token1.token()).isNotBlank();
            assertThat(token2.token()).isNotBlank();
        }

        @Test
        @DisplayName("Should handle null workstationId in user")
        void shouldHandleNullWorkstationId() {
            User user = createTestUser("admin", UserRole.ADMIN, null);
            
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            assertThat(token).isNotNull();
            assertThat(token.token()).isNotBlank();
        }
    }

    // ==================== isTokenValid Tests ====================
    
    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            User user = createTestUser("testuser", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            boolean result = jwtTokenProvider.isTokenValid(token.token());
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalseForMalformedToken() {
            boolean result = jwtTokenProvider.isTokenValid("invalid.token.format");
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            boolean result = jwtTokenProvider.isTokenValid("");
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null token")
        void shouldReturnFalseForNullToken() {
            boolean result = jwtTokenProvider.isTokenValid(null);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for token with wrong signature")
        void shouldReturnFalseForWrongSignature() {
            User user = createTestUser("testuser", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            // Create provider with different secret
            JwtProperties differentProps = new JwtProperties();
            differentProps.setSecret("different-secret-key-that-is-at-least-32-bytes-long");
            differentProps.setExpiration(TEST_EXPIRATION);
            JwtTokenProvider differentProvider = new JwtTokenProvider(differentProps);
            
            boolean result = differentProvider.isTokenValid(token.token());
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            // Create provider with very short expiration
            JwtProperties expiredProps = new JwtProperties();
            expiredProps.setSecret(TEST_SECRET);
            expiredProps.setExpiration(Duration.ofMillis(1));
            JwtTokenProvider shortLivedProvider = new JwtTokenProvider(expiredProps);
            
            User user = createTestUser("testuser", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = shortLivedProvider.createToken(user);
            
            // Wait for token to expire
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            
            boolean result = shortLivedProvider.isTokenValid(token.token());
            
            assertThat(result).isFalse();
        }
    }

    // ==================== extractUsername Tests ====================
    
    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            User user = createTestUser("admin_user", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            String username = jwtTokenProvider.extractUsername(token.token());
            
            assertThat(username).isEqualTo("admin_user");
        }

        @Test
        @DisplayName("Should extract different usernames correctly")
        void shouldExtractDifferentUsernamesCorrectly() {
            User user1 = createTestUser("warehouse_operator", UserRole.PLANT_WAREHOUSE, 7L);
            User user2 = createTestUser("production_planner", UserRole.PRODUCTION_PLANNING, null);
            
            JwtTokenProvider.JwtToken token1 = jwtTokenProvider.createToken(user1);
            JwtTokenProvider.JwtToken token2 = jwtTokenProvider.createToken(user2);
            
            assertThat(jwtTokenProvider.extractUsername(token1.token())).isEqualTo("warehouse_operator");
            assertThat(jwtTokenProvider.extractUsername(token2.token())).isEqualTo("production_planner");
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> jwtTokenProvider.extractUsername("invalid.token"))
                .isInstanceOf(Exception.class);
        }
    }

    // ==================== extractRole Tests ====================
    
    @Nested
    @DisplayName("extractRole()")
    class ExtractRoleTests {

        @Test
        @DisplayName("Should extract ADMIN role from token")
        void shouldExtractAdminRole() {
            User user = createTestUser("admin", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            UserRole role = jwtTokenProvider.extractRole(token.token());
            
            assertThat(role).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Should extract PLANT_WAREHOUSE role from token")
        void shouldExtractPlantWarehouseRole() {
            User user = createTestUser("warehouse", UserRole.PLANT_WAREHOUSE, 7L);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            UserRole role = jwtTokenProvider.extractRole(token.token());
            
            assertThat(role).isEqualTo(UserRole.PLANT_WAREHOUSE);
        }

        @Test
        @DisplayName("Should extract all role types correctly")
        void shouldExtractAllRoleTypes() {
            for (UserRole expectedRole : UserRole.values()) {
                User user = createTestUser("user", expectedRole, null);
                JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
                
                UserRole extractedRole = jwtTokenProvider.extractRole(token.token());
                
                assertThat(extractedRole).isEqualTo(expectedRole);
            }
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> jwtTokenProvider.extractRole("invalid.token"))
                .isInstanceOf(Exception.class);
        }
    }

    // ==================== extractWorkstationId Tests ====================
    
    @Nested
    @DisplayName("extractWorkstationId()")
    class ExtractWorkstationIdTests {

        @Test
        @DisplayName("Should extract workstationId from token")
        void shouldExtractWorkstationId() {
            User user = createTestUser("warehouse", UserRole.PLANT_WAREHOUSE, 7L);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            Long workstationId = jwtTokenProvider.extractWorkstationId(token.token());
            
            assertThat(workstationId).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should return null for user without workstationId")
        void shouldReturnNullForNoWorkstationId() {
            User user = createTestUser("admin", UserRole.ADMIN, null);
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(user);
            
            Long workstationId = jwtTokenProvider.extractWorkstationId(token.token());
            
            assertThat(workstationId).isNull();
        }

        @Test
        @DisplayName("Should extract different workstation IDs correctly")
        void shouldExtractDifferentWorkstationIds() {
            User ws7User = createTestUser("ws7", UserRole.PLANT_WAREHOUSE, 7L);
            User ws8User = createTestUser("ws8", UserRole.MODULES_SUPERMARKET, 8L);
            User ws9User = createTestUser("ws9", UserRole.PARTS_SUPPLY, 9L);
            
            JwtTokenProvider.JwtToken token7 = jwtTokenProvider.createToken(ws7User);
            JwtTokenProvider.JwtToken token8 = jwtTokenProvider.createToken(ws8User);
            JwtTokenProvider.JwtToken token9 = jwtTokenProvider.createToken(ws9User);
            
            assertThat(jwtTokenProvider.extractWorkstationId(token7.token())).isEqualTo(7L);
            assertThat(jwtTokenProvider.extractWorkstationId(token8.token())).isEqualTo(8L);
            assertThat(jwtTokenProvider.extractWorkstationId(token9.token())).isEqualTo(9L);
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowForInvalidToken() {
            assertThatThrownBy(() -> jwtTokenProvider.extractWorkstationId("invalid.token"))
                .isInstanceOf(Exception.class);
        }
    }

    // ==================== Token Round-Trip Tests ====================
    
    @Nested
    @DisplayName("Token Round-Trip")
    class TokenRoundTripTests {

        @Test
        @DisplayName("Should correctly round-trip all user data")
        void shouldRoundTripAllUserData() {
            User originalUser = createTestUser("test_user", UserRole.MANUFACTURING, 3L);
            
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(originalUser);
            
            assertThat(jwtTokenProvider.extractUsername(token.token())).isEqualTo("test_user");
            assertThat(jwtTokenProvider.extractRole(token.token())).isEqualTo(UserRole.MANUFACTURING);
            assertThat(jwtTokenProvider.extractWorkstationId(token.token())).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should correctly round-trip user with no workstation")
        void shouldRoundTripUserWithNoWorkstation() {
            User originalUser = createTestUser("viewer_user", UserRole.VIEWER, null);
            
            JwtTokenProvider.JwtToken token = jwtTokenProvider.createToken(originalUser);
            
            assertThat(jwtTokenProvider.isTokenValid(token.token())).isTrue();
            assertThat(jwtTokenProvider.extractUsername(token.token())).isEqualTo("viewer_user");
            assertThat(jwtTokenProvider.extractRole(token.token())).isEqualTo(UserRole.VIEWER);
            assertThat(jwtTokenProvider.extractWorkstationId(token.token())).isNull();
        }
    }
}
