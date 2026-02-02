package io.life.user_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for User Service
 * 
 * Swagger UI available at: /swagger-ui.html
 * OpenAPI spec at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8012}")
    private String serverPort;

    @Bean
    public OpenAPI userServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("""
                                LIFE (LEGO Integrated Factory Execution) User Service.
                                
                                Handles authentication, authorization, and user management:
                                - User authentication (login/logout)
                                - JWT token generation and validation
                                - User CRUD operations
                                - Role and workstation assignment
                                
                                **Roles:**
                                - ADMIN, PLANT_WAREHOUSE, MODULES_SUPERMARKET
                                - PRODUCTION_PLANNING, PRODUCTION_CONTROL, ASSEMBLY_CONTROL
                                - PARTS_SUPPLY, MANUFACTURING, VIEWER
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LIFE System")
                                .email("support@life-factory.io"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development"),
                        new Server().url("http://user-service:8012").description("Docker Internal")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /api/auth/login")));
    }
}
