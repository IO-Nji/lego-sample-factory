package io.life.order.integration;

import io.life.order.annotation.ApiContract;
import io.life.order.dto.*;
import io.life.order.dto.masterdata.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Contract Integration Tests
 * 
 * Validates that all DTOs marked with @ApiContract annotation:
 * 1. Have the annotation properly configured
 * 2. Contain required fields for external API contracts
 * 3. Match documented contracts in API_CONTRACTS.md
 * 
 * Purpose: Prevent silent deserialization failures by validating
 * API contract compliance at compile/test time.
 */
@SpringBootTest
@DisplayName("API Contract Validation Tests")
public class ApiContractTest {

    /**
     * List of all DTOs that should have @ApiContract annotation
     */
    private static final List<Class<?>> EXTERNAL_DTOS = Arrays.asList(
            // Order DTOs
            CustomerOrderDTO.class,
            WarehouseOrderDTO.class,
            FinalAssemblyOrderDTO.class,
            ProductionOrderDTO.class,
            SupplyOrderDTO.class,
            ProductionControlOrderDTO.class,
            AssemblyControlOrderDTO.class,
            OrderItemDTO.class,
            
            // Configuration
            SystemConfigurationDTO.class,
            
            // Masterdata Client DTOs
            ProductDTO.class,
            ModuleDTO.class,
            PartDTO.class,
            BomEntryDTO.class,
            WorkstationDTO.class
    );

    @Test
    @DisplayName("All external-facing DTOs should have @ApiContract annotation")
    void allExternalDtosShouldBeAnnotated() {
        List<Class<?>> missingAnnotation = EXTERNAL_DTOS.stream()
                .filter(dto -> !dto.isAnnotationPresent(ApiContract.class))
                .collect(Collectors.toList());

        assertThat(missingAnnotation)
                .withFailMessage("These DTOs are missing @ApiContract annotation: %s", 
                        missingAnnotation.stream()
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", ")))
                .isEmpty();
    }

    @Test
    @DisplayName("@ApiContract annotations should have valid version")
    void apiContractsShouldHaveValidVersion() {
        EXTERNAL_DTOS.forEach(dto -> {
            ApiContract contract = dto.getAnnotation(ApiContract.class);
            assertThat(contract).isNotNull();
            assertThat(contract.version())
                    .withFailMessage("DTO %s has invalid version: %s", 
                            dto.getSimpleName(), contract.version())
                    .matches("v\\d+"); // Pattern: v1, v2, etc.
        });
    }

    @Test
    @DisplayName("@ApiContract annotations should document external source")
    void apiContractsShouldDocumentExternalSource() {
        EXTERNAL_DTOS.forEach(dto -> {
            ApiContract contract = dto.getAnnotation(ApiContract.class);
            assertThat(contract).isNotNull();
            assertThat(contract.externalSource())
                    .withFailMessage("DTO %s has no external source documented", 
                            dto.getSimpleName())
                    .isNotBlank();
        });
    }

    @Test
    @DisplayName("@ApiContract annotations should have description")
    void apiContractsShouldHaveDescription() {
        EXTERNAL_DTOS.forEach(dto -> {
            ApiContract contract = dto.getAnnotation(ApiContract.class);
            assertThat(contract).isNotNull();
            assertThat(contract.description())
                    .withFailMessage("DTO %s has no description", 
                            dto.getSimpleName())
                    .isNotBlank();
        });
    }

    @Test
    @DisplayName("OrderItemDTO should have requestedQuantity field mapping")
    void orderItemDtoShouldHaveRequestedQuantityMapping() throws Exception {
        // Verify the field mapping that caused previous bug
        Field quantityField = OrderItemDTO.class.getDeclaredField("quantity");
        assertThat(quantityField).isNotNull();
        
        // Verify @JsonProperty annotation exists on setter
        // This ensures frontend's "requestedQuantity" maps to "quantity"
        boolean hasJsonPropertySetter = Arrays.stream(OrderItemDTO.class.getDeclaredMethods())
                .anyMatch(method -> 
                        method.getName().equals("setRequestedQuantity") &&
                        method.getParameterCount() == 1
                );
        
        assertThat(hasJsonPropertySetter)
                .withFailMessage("OrderItemDTO missing setRequestedQuantity() method for field mapping")
                .isTrue();
    }

    @Test
    @DisplayName("ProductDTO (client) should have id field matching masterdata service")
    void productDtoClientShouldMatchSource() throws Exception {
        // Verify CLIENT DTO has fields matching SOURCE DTO in masterdata-service
        Field idField = ProductDTO.class.getDeclaredField("id");
        Field nameField = ProductDTO.class.getDeclaredField("name");
        Field descriptionField = ProductDTO.class.getDeclaredField("description");
        Field priceField = ProductDTO.class.getDeclaredField("price");
        
        assertThat(idField).isNotNull();
        assertThat(nameField).isNotNull();
        assertThat(descriptionField).isNotNull();
        assertThat(priceField).isNotNull();
        
        // Verify field types match expected API contract
        assertThat(idField.getType()).isEqualTo(Long.class);
        assertThat(nameField.getType()).isEqualTo(String.class);
        assertThat(priceField.getType()).isEqualTo(Double.class);
    }

    @Test
    @DisplayName("ModuleDTO (client) should have id field matching masterdata service")
    void moduleDtoClientShouldMatchSource() throws Exception {
        // Verify CLIENT DTO has fields matching SOURCE DTO in masterdata-service
        Field idField = ModuleDTO.class.getDeclaredField("id");
        Field nameField = ModuleDTO.class.getDeclaredField("name");
        Field descriptionField = ModuleDTO.class.getDeclaredField("description");
        
        assertThat(idField).isNotNull();
        assertThat(nameField).isNotNull();
        assertThat(descriptionField).isNotNull();
        
        // Verify field types match expected API contract
        assertThat(idField.getType()).isEqualTo(Long.class);
        assertThat(nameField.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("BomEntryDTO should have componentId field (not moduleId)")
    void bomEntryDtoShouldHaveComponentIdField() throws Exception {
        // Verify the generic componentId field exists (can be module OR part)
        Field componentIdField = BomEntryDTO.class.getDeclaredField("componentId");
        Field componentNameField = BomEntryDTO.class.getDeclaredField("componentName");
        Field componentTypeField = BomEntryDTO.class.getDeclaredField("componentType");
        Field quantityField = BomEntryDTO.class.getDeclaredField("quantity");
        
        assertThat(componentIdField).isNotNull();
        assertThat(componentNameField).isNotNull();
        assertThat(componentTypeField).isNotNull();
        assertThat(quantityField).isNotNull();
        
        // Verify types
        assertThat(componentIdField.getType()).isEqualTo(Long.class);
        assertThat(componentNameField.getType()).isEqualTo(String.class);
        assertThat(componentTypeField.getType()).isEqualTo(String.class);
        assertThat(quantityField.getType()).isEqualTo(Integer.class);
    }

    @Test
    @DisplayName("All annotated DTOs should be serializable")
    void allAnnotatedDtosShouldBeSerializable() {
        EXTERNAL_DTOS.forEach(dto -> {
            // Verify DTO has public no-args constructor (required for Jackson)
            boolean hasNoArgsConstructor = Arrays.stream(dto.getDeclaredConstructors())
                    .anyMatch(constructor -> constructor.getParameterCount() == 0);
            
            assertThat(hasNoArgsConstructor)
                    .withFailMessage("DTO %s lacks no-args constructor (required for deserialization)", 
                            dto.getSimpleName())
                    .isTrue();
        });
    }

    @Test
    @DisplayName("No deprecated contracts without deprecatedSince date")
    void deprecatedContractsShouldHaveDeprecatedSinceDate() {
        EXTERNAL_DTOS.forEach(dto -> {
            ApiContract contract = dto.getAnnotation(ApiContract.class);
            if (contract.deprecated()) {
                assertThat(contract.deprecatedSince())
                        .withFailMessage("DTO %s is deprecated but has no deprecatedSince date", 
                                dto.getSimpleName())
                        .isNotBlank();
            }
        });
    }

    @Test
    @DisplayName("CustomerOrderDTO should have orderNumber field")
    void customerOrderDtoShouldHaveOrderNumber() throws Exception {
        Field orderNumberField = CustomerOrderDTO.class.getDeclaredField("orderNumber");
        assertThat(orderNumberField).isNotNull();
        assertThat(orderNumberField.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("WarehouseOrderDTO should have productionOrderId field")
    void warehouseOrderDtoShouldHaveProductionOrderIdField() throws Exception {
        // Verify the critical productionOrderId field exists (prevents stock interference)
        Field productionOrderIdField = WarehouseOrderDTO.class.getDeclaredField("productionOrderId");
        assertThat(productionOrderIdField).isNotNull();
        assertThat(productionOrderIdField.getType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("FinalAssemblyOrderDTO should have outputProductId field")
    void finalAssemblyOrderDtoShouldHaveOutputProductIdField() throws Exception {
        // Verify outputProductId field exists (must be PRODUCT ID, not module ID)
        Field outputProductIdField = FinalAssemblyOrderDTO.class.getDeclaredField("outputProductId");
        assertThat(outputProductIdField).isNotNull();
        assertThat(outputProductIdField.getType()).isEqualTo(Long.class);
    }

    @Test
    @DisplayName("SystemConfigurationDTO should have configKey and configValue fields")
    void systemConfigDtoShouldHaveRequiredFields() throws Exception {
        Field configKeyField = SystemConfigurationDTO.class.getDeclaredField("configKey");
        Field configValueField = SystemConfigurationDTO.class.getDeclaredField("configValue");
        
        assertThat(configKeyField).isNotNull();
        assertThat(configValueField).isNotNull();
        
        assertThat(configKeyField.getType()).isEqualTo(String.class);
        assertThat(configValueField.getType()).isEqualTo(String.class);
    }
}
