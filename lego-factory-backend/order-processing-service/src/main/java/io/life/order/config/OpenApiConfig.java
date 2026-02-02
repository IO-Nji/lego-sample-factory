package io.life.order.config;

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
 * OpenAPI configuration for Order Processing Service
 * 
 * Swagger UI available at: /swagger-ui.html
 * OpenAPI spec at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8015}")
    private String serverPort;

    @Bean
    public OpenAPI orderProcessingOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Order Processing Service API")
                        .description("""
                                LIFE (LEGO Integrated Factory Execution) Order Processing Service.
                                
                                Manages the complete order lifecycle including:
                                - Customer Orders (WS-7 Plant Warehouse)
                                - Warehouse Orders (WS-8 Modules Supermarket)
                                - Production Orders & Control Orders
                                - Workstation Orders (WS-1 to WS-6)
                                - Supply Orders (WS-9 Parts Supply)
                                
                                **Business Scenarios:**
                                - Scenario 1: Direct Fulfillment from stock
                                - Scenario 2: Warehouse Order + Final Assembly
                                - Scenario 3: Full Production Pipeline
                                - Scenario 4: High Volume Direct Production
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
                        new Server().url("http://order-processing-service:8015").description("Docker Internal")))
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
