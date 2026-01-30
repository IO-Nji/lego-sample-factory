package io.life.inventory.config;

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
 * OpenAPI configuration for Inventory Service
 * 
 * Swagger UI available at: /swagger-ui.html
 * OpenAPI spec at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8014}")
    private String serverPort;

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("""
                                LIFE (LEGO Integrated Factory Execution) Inventory Service.
                                
                                Manages stock levels across all workstations:
                                - Stock queries by workstation, item type, and item ID
                                - Stock adjustments (credit/debit operations)
                                - Stock ledger history tracking
                                - Inventory validation for order processing
                                
                                **Workstations:**
                                - WS-7: Plant Warehouse (Products)
                                - WS-8: Modules Supermarket (Modules)
                                - WS-9: Parts Supply (Parts/Raw Materials)
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
                        new Server().url("http://inventory-service:8014").description("Docker Internal")))
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
