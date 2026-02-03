package io.life.masterdata.service;

import io.life.masterdata.entity.Part;
import io.life.masterdata.repository.PartRepository;
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
 * Unit tests for PartService
 * 
 * Tests CRUD operations for Part entity:
 * - findAll: retrieve all parts
 * - findById: retrieve part by ID
 * - save: create or update part
 * - deleteById: delete part by ID
 * 
 * Parts are stored at WS-9 (Parts Supply) and produced at WS-1, WS-2, WS-3.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PartService Tests")
class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private PartService partService;

    private Part gearPart;
    private Part motorPart;

    @BeforeEach
    void setUp() {
        gearPart = new Part(
                1L,
                "Steel Gear",
                "High-quality steel gear component",
                "GEAR",
                5.99
        );

        motorPart = new Part(
                2L,
                "DC Motor",
                "12V DC motor for assembly",
                "MOTOR",
                15.99
        );
    }

    // ========================================================================
    // findAll Tests
    // ========================================================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return all parts")
        void shouldReturnAllParts() {
            when(partRepository.findAll())
                    .thenReturn(Arrays.asList(gearPart, motorPart));

            List<Part> result = partService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Part::getName)
                    .containsExactly("Steel Gear", "DC Motor");
            verify(partRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no parts exist")
        void shouldReturnEmptyListWhenNoParts() {
            when(partRepository.findAll())
                    .thenReturn(List.of());

            List<Part> result = partService.findAll();

            assertThat(result).isEmpty();
            verify(partRepository).findAll();
        }

        @Test
        @DisplayName("Should return parts with categories")
        void shouldReturnPartsWithCategories() {
            when(partRepository.findAll())
                    .thenReturn(Arrays.asList(gearPart, motorPart));

            List<Part> result = partService.findAll();

            assertThat(result.get(0).getCategory()).isEqualTo("GEAR");
            assertThat(result.get(1).getCategory()).isEqualTo("MOTOR");
        }
    }

    // ========================================================================
    // findById Tests
    // ========================================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return part when found")
        void shouldReturnPartWhenFound() {
            when(partRepository.findById(1L))
                    .thenReturn(Optional.of(gearPart));

            Optional<Part> result = partService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Steel Gear");
            assertThat(result.get().getCategory()).isEqualTo("GEAR");
            assertThat(result.get().getUnitCost()).isEqualTo(5.99);
            verify(partRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when part not found")
        void shouldReturnEmptyWhenNotFound() {
            when(partRepository.findById(999L))
                    .thenReturn(Optional.empty());

            Optional<Part> result = partService.findById(999L);

            assertThat(result).isEmpty();
            verify(partRepository).findById(999L);
        }

        @Test
        @DisplayName("Should handle null ID gracefully")
        void shouldHandleNullId() {
            when(partRepository.findById(null))
                    .thenReturn(Optional.empty());

            Optional<Part> result = partService.findById(null);

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
        @DisplayName("Should save new part")
        void shouldSaveNewPart() {
            Part newPart = new Part(null, "New Part", "Description", "MISC", 9.99);
            Part savedPart = new Part(3L, "New Part", "Description", "MISC", 9.99);
            
            when(partRepository.save(newPart))
                    .thenReturn(savedPart);

            Part result = partService.save(newPart);

            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getName()).isEqualTo("New Part");
            verify(partRepository).save(newPart);
        }

        @Test
        @DisplayName("Should update existing part")
        void shouldUpdateExistingPart() {
            gearPart.setUnitCost(7.99);
            
            when(partRepository.save(gearPart))
                    .thenReturn(gearPart);

            Part result = partService.save(gearPart);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUnitCost()).isEqualTo(7.99);
            verify(partRepository).save(gearPart);
        }

        @Test
        @DisplayName("Should save part with all fields")
        void shouldSavePartWithAllFields() {
            Part fullPart = new Part(
                    null,
                    "Complete Part",
                    "Full description with details",
                    "BEARING",
                    12.50
            );
            Part savedPart = new Part(
                    4L,
                    "Complete Part",
                    "Full description with details",
                    "BEARING",
                    12.50
            );

            when(partRepository.save(fullPart))
                    .thenReturn(savedPart);

            Part result = partService.save(fullPart);

            assertThat(result.getId()).isEqualTo(4L);
            assertThat(result.getName()).isEqualTo("Complete Part");
            assertThat(result.getDescription()).isEqualTo("Full description with details");
            assertThat(result.getCategory()).isEqualTo("BEARING");
            assertThat(result.getUnitCost()).isEqualTo(12.50);
        }
    }

    // ========================================================================
    // deleteById Tests
    // ========================================================================

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("Should delete part by ID")
        void shouldDeletePartById() {
            doNothing().when(partRepository).deleteById(1L);

            partService.deleteById(1L);

            verify(partRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should handle non-existent ID gracefully")
        void shouldHandleNonExistentId() {
            doNothing().when(partRepository).deleteById(999L);

            partService.deleteById(999L);

            verify(partRepository).deleteById(999L);
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
            when(partRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> partService.findAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should propagate repository exception on save")
        void shouldPropagateExceptionOnSave() {
            when(partRepository.save(any(Part.class)))
                    .thenThrow(new RuntimeException("Constraint violation"));

            assertThatThrownBy(() -> partService.save(gearPart))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Constraint violation");
        }

        @Test
        @DisplayName("Should propagate repository exception on delete")
        void shouldPropagateExceptionOnDelete() {
            doThrow(new RuntimeException("Delete failed"))
                    .when(partRepository).deleteById(1L);

            assertThatThrownBy(() -> partService.deleteById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete failed");
        }
    }
}
