package io.life.masterdata.service;

import io.life.masterdata.entity.Product;
import io.life.masterdata.repository.ProductRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService
 * 
 * Tests CRUD operations for Product entity:
 * - findAll: retrieve all products
 * - findById: retrieve product by ID
 * - save: create or update product
 * - deleteById: delete product by ID
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testProduct = new Product(
                1L,
                "LEGO Model Car",
                "A detailed LEGO car model with gear and motor systems",
                99.99,
                180
        );

        testProduct2 = new Product(
                2L,
                "LEGO Robot Arm",
                "Industrial robot arm with servo motors",
                149.99,
                240
        );
    }

    // ========================================================================
    // findAll Tests
    // ========================================================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return all products")
        void shouldReturnAllProducts() {
            when(productRepository.findAll())
                    .thenReturn(Arrays.asList(testProduct, testProduct2));

            List<Product> result = productService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Product::getName)
                    .containsExactly("LEGO Model Car", "LEGO Robot Arm");
            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyListWhenNoProducts() {
            when(productRepository.findAll())
                    .thenReturn(List.of());

            List<Product> result = productService.findAll();

            assertThat(result).isEmpty();
            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("Should return single product when only one exists")
        void shouldReturnSingleProduct() {
            when(productRepository.findAll())
                    .thenReturn(List.of(testProduct));

            List<Product> result = productService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("LEGO Model Car");
        }
    }

    // ========================================================================
    // findById Tests
    // ========================================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return product when found")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(1L))
                    .thenReturn(Optional.of(testProduct));

            Optional<Product> result = productService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("LEGO Model Car");
            assertThat(result.get().getPrice()).isEqualTo(99.99);
            assertThat(result.get().getEstimatedTimeMinutes()).isEqualTo(180);
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when product not found")
        void shouldReturnEmptyWhenNotFound() {
            when(productRepository.findById(999L))
                    .thenReturn(Optional.empty());

            Optional<Product> result = productService.findById(999L);

            assertThat(result).isEmpty();
            verify(productRepository).findById(999L);
        }

        @Test
        @DisplayName("Should handle null ID gracefully")
        void shouldHandleNullId() {
            when(productRepository.findById(null))
                    .thenReturn(Optional.empty());

            Optional<Product> result = productService.findById(null);

            assertThat(result).isEmpty();
        }
    }

    // ========================================================================
    // save Tests
    // ========================================================================

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should save new product")
        void shouldSaveNewProduct() {
            Product newProduct = new Product(null, "New Product", "Description", 49.99, 60);
            Product savedProduct = new Product(3L, "New Product", "Description", 49.99, 60);
            
            when(productRepository.save(newProduct))
                    .thenReturn(savedProduct);

            Product result = productService.save(newProduct);

            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getName()).isEqualTo("New Product");
            verify(productRepository).save(newProduct);
        }

        @Test
        @DisplayName("Should update existing product")
        void shouldUpdateExistingProduct() {
            testProduct.setPrice(89.99);
            
            when(productRepository.save(testProduct))
                    .thenReturn(testProduct);

            Product result = productService.save(testProduct);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getPrice()).isEqualTo(89.99);
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Should save product with all fields")
        void shouldSaveProductWithAllFields() {
            Product fullProduct = new Product(
                    null,
                    "Complete Product",
                    "Full description with details",
                    199.99,
                    360
            );
            Product savedProduct = new Product(
                    4L,
                    "Complete Product",
                    "Full description with details",
                    199.99,
                    360
            );

            when(productRepository.save(fullProduct))
                    .thenReturn(savedProduct);

            Product result = productService.save(fullProduct);

            assertThat(result.getId()).isEqualTo(4L);
            assertThat(result.getName()).isEqualTo("Complete Product");
            assertThat(result.getDescription()).isEqualTo("Full description with details");
            assertThat(result.getPrice()).isEqualTo(199.99);
            assertThat(result.getEstimatedTimeMinutes()).isEqualTo(360);
        }
    }

    // ========================================================================
    // deleteById Tests
    // ========================================================================

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("Should delete product by ID")
        void shouldDeleteProductById() {
            doNothing().when(productRepository).deleteById(1L);

            productService.deleteById(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should handle non-existent ID gracefully")
        void shouldHandleNonExistentId() {
            doNothing().when(productRepository).deleteById(999L);

            productService.deleteById(999L);

            verify(productRepository).deleteById(999L);
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
            when(productRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> productService.findAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should propagate repository exception on save")
        void shouldPropagateExceptionOnSave() {
            when(productRepository.save(any(Product.class)))
                    .thenThrow(new RuntimeException("Constraint violation"));

            assertThatThrownBy(() -> productService.save(testProduct))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Constraint violation");
        }

        @Test
        @DisplayName("Should propagate repository exception on delete")
        void shouldPropagateExceptionOnDelete() {
            doThrow(new RuntimeException("Delete failed"))
                    .when(productRepository).deleteById(1L);

            assertThatThrownBy(() -> productService.deleteById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete failed");
        }
    }
}
