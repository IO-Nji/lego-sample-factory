package io.life.masterdata.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.life.masterdata.dto.ModuleDto;
import io.life.masterdata.dto.PartDto;
import io.life.masterdata.dto.WorkstationDto;
import io.life.masterdata.entity.Module;
import io.life.masterdata.entity.ModulePart;
import io.life.masterdata.entity.Part;
import io.life.masterdata.service.ModulePartService;
import io.life.masterdata.service.ModuleService;
import io.life.masterdata.service.PartService;
import io.life.masterdata.service.WorkstationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/masterdata")
@Tag(name = "Masterdata", description = "Modules, parts, and workstation reference data management")
public class MasterdataController {

    private final ModuleService moduleService;
    private final PartService partService;
    private final WorkstationService workstationService;
    private final ModulePartService modulePartService;

    public MasterdataController(ModuleService moduleService,
            PartService partService, WorkstationService workstationService,
            ModulePartService modulePartService) {
        this.moduleService = moduleService;
        this.partService = partService;
        this.workstationService = workstationService;
        this.modulePartService = modulePartService;
    }

    @Operation(summary = "Get all modules", description = "Retrieve all modules (subassemblies) in the system")
    @ApiResponse(responseCode = "200", description = "List of modules")
    @GetMapping("/modules")
    public List<ModuleDto> getModules() {
        return moduleService.findAll().stream()
            .map(this::toModuleDto)
            .collect(Collectors.toList());
    }

    @Operation(summary = "Get module by ID", description = "Retrieve a specific module by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module found"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleDto> getModuleById(
            @Parameter(description = "Module ID") @PathVariable Long id) {
        return moduleService.findById(id)
            .map(module -> ResponseEntity.ok(toModuleDto(module)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get module parts (BOM)", description = "Retrieve all parts required to build a module")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of module-part relationships"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @GetMapping("/modules/{id}/parts")
    public ResponseEntity<List<ModulePart>> getModuleParts(
            @Parameter(description = "Module ID") @PathVariable Long id) {
        log.debug("Fetching parts for module ID: {}", id);
        return moduleService.findById(id)
            .map(module -> {
                List<ModulePart> moduleParts = modulePartService.findByModuleId(id);
                log.debug("Found {} parts for module ID: {}", moduleParts.size(), id);
                return ResponseEntity.ok(moduleParts);
            })
            .orElseGet(() -> {
                log.warn("Module not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            });
    }

    @Operation(summary = "Get all workstations", description = "Retrieve all factory workstations (WS-1 to WS-9)")
    @ApiResponse(responseCode = "200", description = "List of workstations")
    @GetMapping("/workstations")
    public List<WorkstationDto> getWorkstations() {
        return workstationService.findAll();
    }

    @Operation(summary = "Get workstation by ID", description = "Retrieve a specific workstation by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workstation found"),
        @ApiResponse(responseCode = "404", description = "Workstation not found")
    })
    @GetMapping("/workstations/{id}")
    public ResponseEntity<WorkstationDto> getWorkstationById(
            @Parameter(description = "Workstation ID (1-9)") @PathVariable Long id) {
        WorkstationDto workstation = workstationService.findById(id);
        return workstation != null ? ResponseEntity.ok(workstation) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get all parts", description = "Retrieve all raw parts/components in the system")
    @ApiResponse(responseCode = "200", description = "List of parts")
    @GetMapping("/parts")
    public List<PartDto> getParts() {
        return partService.findAll().stream()
            .map(this::toPartDto)
            .collect(Collectors.toList());
    }

    @Operation(summary = "Get part by ID", description = "Retrieve a specific part by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Part found"),
        @ApiResponse(responseCode = "404", description = "Part not found")
    })
    @GetMapping("/parts/{id}")
    public ResponseEntity<PartDto> getPartById(
            @Parameter(description = "Part ID") @PathVariable Long id) {
        return partService.findById(id)
            .map(part -> ResponseEntity.ok(toPartDto(part)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Modules CRUD ---
    @Operation(summary = "Create module", description = "Create a new module")
    @ApiResponse(responseCode = "200", description = "Module created")
    @PostMapping("/modules")
    public ResponseEntity<ModuleDto> createModule(@RequestBody ModuleDto dto) {
        Module entity = toModuleEntity(dto);
        Module saved = moduleService.save(entity);
        return ResponseEntity.ok(toModuleDto(saved));
    }

    @Operation(summary = "Update module", description = "Update an existing module")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module updated"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleDto> updateModule(
            @Parameter(description = "Module ID") @PathVariable Long id, 
            @RequestBody ModuleDto dto) {
        return moduleService.findById(id)
            .map(existing -> {
                existing.setName(dto.getName());
                existing.setDescription(dto.getDescription());
                existing.setType(dto.getType());
                Module saved = moduleService.save(existing);
                return ResponseEntity.ok(toModuleDto(saved));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete module", description = "Delete a module by ID")
    @ApiResponse(responseCode = "204", description = "Module deleted")
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(
            @Parameter(description = "Module ID") @PathVariable Long id) {
        moduleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Parts CRUD ---
    @Operation(summary = "Create part", description = "Create a new part")
    @ApiResponse(responseCode = "200", description = "Part created")
    @PostMapping("/parts")
    public ResponseEntity<PartDto> createPart(@RequestBody PartDto dto) {
        Part entity = toPartEntity(dto);
        Part saved = partService.save(entity);
        return ResponseEntity.ok(toPartDto(saved));
    }

    @Operation(summary = "Update part", description = "Update an existing part")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Part updated"),
        @ApiResponse(responseCode = "404", description = "Part not found")
    })
    @PutMapping("/parts/{id}")
    public ResponseEntity<PartDto> updatePart(
            @Parameter(description = "Part ID") @PathVariable Long id, 
            @RequestBody PartDto dto) {
        return partService.findById(id)
            .map(existing -> {
                existing.setName(dto.getName());
                existing.setDescription(dto.getDescription());
                existing.setCategory(dto.getCategory());
                existing.setUnitCost(dto.getUnitCost());
                Part saved = partService.save(existing);
                return ResponseEntity.ok(toPartDto(saved));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete part", description = "Delete a part by ID")
    @ApiResponse(responseCode = "204", description = "Part deleted")
    @DeleteMapping("/parts/{id}")
    public ResponseEntity<Void> deletePart(
            @Parameter(description = "Part ID") @PathVariable Long id) {
        partService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Workstations CRUD ---
    @Operation(summary = "Create workstation", description = "Create a new workstation")
    @ApiResponse(responseCode = "200", description = "Workstation created")
    @PostMapping("/workstations")
    public ResponseEntity<WorkstationDto> createWorkstation(@RequestBody WorkstationDto dto) {
        WorkstationDto saved = workstationService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Update workstation", description = "Update an existing workstation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workstation updated"),
        @ApiResponse(responseCode = "404", description = "Workstation not found")
    })
    @PutMapping("/workstations/{id}")
    public ResponseEntity<WorkstationDto> updateWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long id, 
            @RequestBody WorkstationDto dto) {
        WorkstationDto existing = workstationService.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        dto.setId(id);
        WorkstationDto saved = workstationService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Delete workstation", description = "Delete a workstation by ID")
    @ApiResponse(responseCode = "204", description = "Workstation deleted")
    @DeleteMapping("/workstations/{id}")
    public ResponseEntity<Void> deleteWorkstation(
            @Parameter(description = "Workstation ID") @PathVariable Long id) {
        workstationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ModuleDto toModuleDto(Module module) {
        return new ModuleDto(
            module.getId(),
            module.getName(),
            module.getDescription(),
            module.getType(),
            module.getProductionWorkstationId()
        );
    }

    private PartDto toPartDto(Part part) {
        return new PartDto(
            part.getId(),
            part.getName(),
            part.getDescription(),
            part.getCategory(),
            part.getUnitCost()
        );
    }

    private Module toModuleEntity(ModuleDto dto) {
        Module m = new Module();
        m.setId(dto.getId());
        m.setName(dto.getName());
        m.setDescription(dto.getDescription());
        m.setType(dto.getType());
        return m;
    }

    private Part toPartEntity(PartDto dto) {
        Part p = new Part();
        p.setId(dto.getId());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setCategory(dto.getCategory());
        p.setUnitCost(dto.getUnitCost());
        return p;
    }
}
