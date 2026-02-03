package io.life.user_service.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void defaultAdminCredentialsAuthenticateSuccessfully() {
        // UserInitializer creates lego_admin with password "password"
        Authentication result = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken("lego_admin", "password"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("lego_admin");
    }
}
