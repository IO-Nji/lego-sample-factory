package io.life.masterdata.service;

import io.life.masterdata.entity.ProductModule;
import io.life.masterdata.repository.ProductModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductModuleService
 * 
 * Tests BOM (Bill of Materials) operations linking Products to Modules:
 * - findAll: retrieve all product-module relationships
 * - findByProductId: get modules for a product (BOM lookup)
 * - findByModuleId: get products using a module (reverse BOM lookup)
 * - save: create or update product-module relationship
 * 
 * Critical for order processing - determines which modules to produce for products.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductModuleService Tests")
class ProductModuleServiceTest {

    @Mock
    private ProductModuleRepository productModuleRepository;

    @InjectMocks
    private ProductModuleService productModuleService;

    private ProductModule productModule1;
    private ProductModule productModule2;
    private ProductModule productModule3;

    @BeforeEach
    void setUp() {
        // Product 1 (Model Car) requires Gear Module (qty 2) and Motor Module (qty 1)
        productModule1 = new ProductModule(1L, 1L, 1L, 2);  // id, productId, moduleId, quantity
        productModule2 = new ProductModule(2L, 1L, 2L, 1);  // Product 1 needs Motor Module
        
        // Product 2 (Robot Arm) requires Motor Module (qty 3)
        productModule3 = new ProductModule(3L, 2L, 2L, 3);
    }

    // ========================================================================
    // findAll Tests
    // ========================================================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return all product-module relationships")
        void shouldReturnAllProductModules() {
            when(productModuleRepository.findAll())
                    .thenReturn(Arrays.asList(productModule1, productModule2, productModule3));

            List<ProductModule> result = productModuleService.findAll();

            assertThat(result).hasSize(3);
            verify(productModuleRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no relationships exist")
        void shouldReturnEmptyListWhenNoRelationships() {
            when(productModuleRepository.findAll())
                    .thenReturn(List.of());

            List<ProductModule> result = productModuleService.findAll();

            assertThat(result).isEmpty();
            verify(productModuleRepository).findAll();
        }
    }

    // ========================================================================
    // findByProductId Tests (BOM Lookup)
    // ========================================================================

    @Nested
    @DisplayName("findByProductId (BOM Lookup)")
    class FindByProductId {

        @Test
        @DisplayName("Should return modules for product (BOM)")
        void shouldReturnModulesForProduct() {
            when(productModuleRepository.findByProductId(1L))
                    .thenReturn(Arrays.asList(productModule1, productModule2));

            List<ProductModule> result = productModuleService.findByProductId(1L);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(pm -> pm.getProductId().equals(1L));
            verify(productModuleRepository).findByProductId(1L);
        }

        @Test
        @DisplayName("Should return correct quantities for each module")
        void shouldReturnCorrectQuantities() {
            when(productModuleRepository.findByProductId(1L))
                    .thenReturn(Arrays.asList(productModule1, productModule2));

            List<ProductModule> result = productModuleService.findByProductId(1L);

            assertThat(result).extracting(ProductModule::getModuleId)
                    .containsExactly(1L, 2L);
            assertThat(result).extracting(ProductModule::getQuantity)
                    .containsExactly(2, 1);
        }

        @Test
        @DisplayName("Should return empty list for product with no modules")
        void shouldReturnEmptyListForProductWithNoModules() {
            when(productModuleRepository.findByProductId(999L))
                    .thenReturn(List.of());

            List<ProductModule> result = productModuleService.findByProductId(999L);

            assertThat(result).isEmpty();
            verify(productModuleRepository).findByProductId(999L);
        }

        @Test
        @DisplayName("Should return single module for simple product")
        void shouldReturnSingleModuleForSimpleProduct() {
            when(productModuleRepository.findByProductId(2L))
                    .thenReturn(List.of(productModule3));

            List<ProductModule> result = productModuleService.findByProductId(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getModuleId()).isEqualTo(2L);
            assertThat(result.get(0).getQuantity()).isEqualTo(3);
        }
    }

    // ========================================================================
    // findByModuleId Tests (Reverse BOM Lookup)
    // ========================================================================

    @Nested
    @DisplayName("findByModuleId (Reverse BOM Lookup)")
    class FindByModuleId {

        @Test
        @DisplayName("Should return products using a module")
        void shouldReturnProductsUsingModule() {
            // Motor Module (ID 2) is used by both products
            when(productModuleRepository.findByModuleId(2L))
                    .thenReturn(Arrays.asList(productModule2, productModule3));

            List<ProductModule> result = productModuleService.findByModuleId(2L);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(pm -> pm.getModuleId().equals(2L));
            verify(productModuleRepository).findByModuleId(2L);
        }

        @Test
        @DisplayName("Should return products with different quantities")
        void shouldReturnProductsWithDifferentQuantities() {
            when(productModuleRepository.findByModuleId(2L))
                    .thenReturn(Arrays.asList(productModule2, productModule3));

            List<ProductModule> result = productModuleService.findByModuleId(2L);

            assertThat(result).extracting(ProductModule::getProductId)
                    .containsExactly(1L, 2L);
            assertThat(result).extracting(ProductModule::getQuantity)
                    .containsExactly(1, 3);
        }

        @Test
        @DisplayName("Should return empty list for module not used in any product")
        void shouldReturnEmptyListForUnusedModule() {
            when(productModuleRepository.findByModuleId(999L))
                    .thenReturn(List.of());

            List<ProductModule> result = productModuleService.findByModuleId(999L);

            assertThat(result).isEmpty();
            verify(productModuleRepository).findByModuleId(999L);
        }

        @Test
        @DisplayName("Should return single product for exclusive module")
        void shouldReturnSingleProductForExclusiveModule() {
            // Gear Module (ID 1) is only used by Product 1
            when(productModuleRepository.findByModuleId(1L))
                    .thenReturn(List.of(productModule1));

            List<ProductModule> result = productModuleService.findByModuleId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProductId()).isEqualTo(1L);
            assertThat(result.get(0).getQuantity()).isEqualTo(2);
        }
    }

    // ========================================================================
    // save Tests
    // ========================================================================

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should save new product-module relationship")
        void shouldSaveNewRelationship() {
            ProductModule newPM = new ProductModule(3L, 3L, 2);  // Product 3 needs Module 3
            ProductModule savedPM = new ProductModule(4L, 3L, 3L, 2);
            
            when(productModuleRepository.save(newPM))
                    .thenReturn(savedPM);

            ProductModule result = productModuleService.save(newPM);

            assertThat(result.getId()).isEqualTo(4L);
            assertThat(result.getProductId()).isEqualTo(3L);
            assertThat(result.getModuleId()).isEqualTo(3L);
            assertThat(result.getQuantity()).isEqualTo(2);
            verify(productModuleRepository).save(newPM);
        }

        @Test
        @DisplayName("Should update existing relationship quantity")
        void shouldUpdateExistingRelationship() {
            productModule1.setQuantity(5);  // Change quantity from 2 to 5
            
            when(productModuleRepository.save(productModule1))
                    .thenReturn(productModule1);

            ProductModule result = productModuleService.save(productModule1);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getQuantity()).isEqualTo(5);
            verify(productModuleRepository).save(productModule1);
        }

        @Test
        @DisplayName("Should save relationship with zero quantity")
        void shouldSaveRelationshipWithZeroQuantity() {
            ProductModule zeroPM = new ProductModule(null, 4L, 4L, 0);
            ProductModule savedPM = new ProductModule(5L, 4L, 4L, 0);

            when(productModuleRepository.save(zeroPM))
                    .thenReturn(savedPM);

            ProductModule result = productModuleService.save(zeroPM);

            assertThat(result.getQuantity()).isEqualTo(0);
        }
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should propagate repository exception on findAll")
        void shouldPropagateExceptionOnFindAll() {
            when(productModuleRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> productModuleService.findAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should propagate repository exception on findByProductId")
        void shouldPropagateExceptionOnFindByProductId() {
            when(productModuleRepository.findByProductId(1L))
                    .thenThrow(new RuntimeException("Query failed"));

            assertThatThrownBy(() -> productModuleService.findByProductId(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Query failed");
        }

        @Test
        @DisplayName("Should propagate repository exception on save")
        void shouldPropagateExceptionOnSave() {
            when(productModuleRepository.save(any(ProductModule.class)))
                    .thenThrow(new RuntimeException("Constraint violation"));

            assertThatThrownBy(() -> productModuleService.save(productModule1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Constraint violation");
        }
    }
}
