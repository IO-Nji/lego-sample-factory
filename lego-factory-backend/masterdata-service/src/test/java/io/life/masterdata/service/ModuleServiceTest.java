package io.life.masterdata.service;

import io.life.masterdata.entity.Module;
import io.life.masterdata.repository.ModuleRepository;
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
 * Unit tests for ModuleService
 * 
 * Tests CRUD operations for Module entity:
 * - findAll: retrieve all modules
 * - findById: retrieve module by ID
 * - save: create or update module
 * - deleteById: delete module by ID
 * 
 * Modules are assembled at specific workstations (WS-4, WS-5) and stored at WS-8.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleService Tests")
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;

    @InjectMocks
    private ModuleService moduleService;

    private Module gearModule;
    private Module motorModule;

    @BeforeEach
    void setUp() {
        gearModule = new Module(
                1L,
                "Gear Module",
                "Complete gear assembly with gears, shafts, and bearings",
                "GEAR",
                4  // WS-4 Gear Assembly
        );

        motorModule = new Module(
                2L,
                "Motor Module",
                "Motor assembly with motor, wires, and housing",
                "MOTOR",
                5  // WS-5 Motor Assembly
        );
    }

    // ========================================================================
    // findAll Tests
    // ========================================================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return all modules")
        void shouldReturnAllModules() {
            when(moduleRepository.findAll())
                    .thenReturn(Arrays.asList(gearModule, motorModule));

            List<Module> result = moduleService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Module::getName)
                    .containsExactly("Gear Module", "Motor Module");
            verify(moduleRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no modules exist")
        void shouldReturnEmptyListWhenNoModules() {
            when(moduleRepository.findAll())
                    .thenReturn(List.of());

            List<Module> result = moduleService.findAll();

            assertThat(result).isEmpty();
            verify(moduleRepository).findAll();
        }

        @Test
        @DisplayName("Should return modules with correct production workstation IDs")
        void shouldReturnModulesWithWorkstationIds() {
            when(moduleRepository.findAll())
                    .thenReturn(Arrays.asList(gearModule, motorModule));

            List<Module> result = moduleService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getProductionWorkstationId()).isEqualTo(4);
            assertThat(result.get(1).getProductionWorkstationId()).isEqualTo(5);
        }
    }

    // ========================================================================
    // findById Tests
    // ========================================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return module when found")
        void shouldReturnModuleWhenFound() {
            when(moduleRepository.findById(1L))
                    .thenReturn(Optional.of(gearModule));

            Optional<Module> result = moduleService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Gear Module");
            assertThat(result.get().getType()).isEqualTo("GEAR");
            assertThat(result.get().getProductionWorkstationId()).isEqualTo(4);
            verify(moduleRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when module not found")
        void shouldReturnEmptyWhenNotFound() {
            when(moduleRepository.findById(999L))
                    .thenReturn(Optional.empty());

            Optional<Module> result = moduleService.findById(999L);

            assertThat(result).isEmpty();
            verify(moduleRepository).findById(999L);
        }

        @Test
        @DisplayName("Should handle null ID gracefully")
        void shouldHandleNullId() {
            when(moduleRepository.findById(null))
                    .thenReturn(Optional.empty());

            Optional<Module> result = moduleService.findById(null);

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
        @DisplayName("Should save new module")
        void shouldSaveNewModule() {
            Module newModule = new Module(null, "New Module", "New description", "CUSTOM", 3);
            Module savedModule = new Module(3L, "New Module", "New description", "CUSTOM", 3);
            
            when(moduleRepository.save(newModule))
                    .thenReturn(savedModule);

            Module result = moduleService.save(newModule);

            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getName()).isEqualTo("New Module");
            verify(moduleRepository).save(newModule);
        }

        @Test
        @DisplayName("Should update existing module")
        void shouldUpdateExistingModule() {
            gearModule.setDescription("Updated gear description");
            
            when(moduleRepository.save(gearModule))
                    .thenReturn(gearModule);

            Module result = moduleService.save(gearModule);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDescription()).isEqualTo("Updated gear description");
            verify(moduleRepository).save(gearModule);
        }

        @Test
        @DisplayName("Should save module with correct workstation assignment")
        void shouldSaveModuleWithWorkstationAssignment() {
            Module moduleForWS4 = new Module(
                    null,
                    "Precision Gear",
                    "High-precision gear module",
                    "GEAR",
                    4  // Gear Assembly workstation
            );
            Module savedModule = new Module(
                    5L,
                    "Precision Gear",
                    "High-precision gear module",
                    "GEAR",
                    4
            );

            when(moduleRepository.save(moduleForWS4))
                    .thenReturn(savedModule);

            Module result = moduleService.save(moduleForWS4);

            assertThat(result.getProductionWorkstationId()).isEqualTo(4);
            assertThat(result.getType()).isEqualTo("GEAR");
        }
    }

    // ========================================================================
    // deleteById Tests
    // ========================================================================

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("Should delete module by ID")
        void shouldDeleteModuleById() {
            doNothing().when(moduleRepository).deleteById(1L);

            moduleService.deleteById(1L);

            verify(moduleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should handle non-existent ID gracefully")
        void shouldHandleNonExistentId() {
            doNothing().when(moduleRepository).deleteById(999L);

            moduleService.deleteById(999L);

            verify(moduleRepository).deleteById(999L);
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
            when(moduleRepository.findAll())
                    .thenThrow(new RuntimeException("Database connection failed"));

            assertThatThrownBy(() -> moduleService.findAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should propagate repository exception on save")
        void shouldPropagateExceptionOnSave() {
            when(moduleRepository.save(any(Module.class)))
                    .thenThrow(new RuntimeException("Constraint violation"));

            assertThatThrownBy(() -> moduleService.save(gearModule))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Constraint violation");
        }

        @Test
        @DisplayName("Should propagate repository exception on delete")
        void shouldPropagateExceptionOnDelete() {
            doThrow(new RuntimeException("Delete failed"))
                    .when(moduleRepository).deleteById(1L);

            assertThatThrownBy(() -> moduleService.deleteById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete failed");
        }
    }
}
