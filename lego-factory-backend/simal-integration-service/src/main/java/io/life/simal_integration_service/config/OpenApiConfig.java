package io.life.simal_integration_service.config;

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
 * OpenAPI configuration for SimAL Integration Service
 * 
 * Swagger UI available at: /swagger-ui.html
 * OpenAPI spec at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8016}")
    private String serverPort;

    @Bean
    public OpenAPI simalIntegrationOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("SimAL Integration Service API")
                        .description("""
                                LIFE (LEGO Integrated Factory Execution) SimAL Integration Service.
                                
                                Provides production scheduling and Gantt chart visualization:
                                - Production order scheduling
                                - Control order creation and dispatch
                                - Gantt chart data generation
                                - Workstation capacity planning
                                - Schedule optimization
                                
                                **SimAL** = Simulation and Scheduling Algorithm Layer
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
                        new Server().url("http://simal-integration-service:8016").description("Docker Internal")))
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
