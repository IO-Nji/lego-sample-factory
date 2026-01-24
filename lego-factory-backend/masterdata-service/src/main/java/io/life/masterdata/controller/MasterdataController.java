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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/masterdata")
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

    @GetMapping("/modules")
    public List<ModuleDto> getModules() {
        return moduleService.findAll().stream()
            .map(this::toModuleDto)
            .collect(Collectors.toList());
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleDto> getModuleById(@PathVariable Long id) {
        return moduleService.findById(id)
            .map(module -> ResponseEntity.ok(toModuleDto(module)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/modules/{id}/parts")
    public ResponseEntity<List<ModulePart>> getModuleParts(@PathVariable Long id) {
        log.debug("Fetching parts for module ID: {}", id);
        // Verify module exists
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

    @GetMapping("/workstations")
    public List<WorkstationDto> getWorkstations() {
        return workstationService.findAll();
    }

    @GetMapping("/workstations/{id}")
    public ResponseEntity<WorkstationDto> getWorkstationById(@PathVariable Long id) {
        WorkstationDto workstation = workstationService.findById(id);
        return workstation != null ? ResponseEntity.ok(workstation) : ResponseEntity.notFound().build();
    }

    @GetMapping("/parts")
    public List<PartDto> getParts() {
        return partService.findAll().stream()
            .map(this::toPartDto)
            .collect(Collectors.toList());
    }

    @GetMapping("/parts/{id}")
    public ResponseEntity<PartDto> getPartById(@PathVariable Long id) {
        return partService.findById(id)
            .map(part -> ResponseEntity.ok(toPartDto(part)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Modules CRUD ---
    @PostMapping("/modules")
    public ResponseEntity<ModuleDto> createModule(@RequestBody ModuleDto dto) {
        Module entity = toModuleEntity(dto);
        Module saved = moduleService.save(entity);
        return ResponseEntity.ok(toModuleDto(saved));
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleDto> updateModule(@PathVariable Long id, @RequestBody ModuleDto dto) {
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

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        moduleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Parts CRUD ---
    @PostMapping("/parts")
    public ResponseEntity<PartDto> createPart(@RequestBody PartDto dto) {
        Part entity = toPartEntity(dto);
        Part saved = partService.save(entity);
        return ResponseEntity.ok(toPartDto(saved));
    }

    @PutMapping("/parts/{id}")
    public ResponseEntity<PartDto> updatePart(@PathVariable Long id, @RequestBody PartDto dto) {
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

    @DeleteMapping("/parts/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        partService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Workstations CRUD ---
    @PostMapping("/workstations")
    public ResponseEntity<WorkstationDto> createWorkstation(@RequestBody WorkstationDto dto) {
        WorkstationDto saved = workstationService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/workstations/{id}")
    public ResponseEntity<WorkstationDto> updateWorkstation(@PathVariable Long id, @RequestBody WorkstationDto dto) {
        WorkstationDto existing = workstationService.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        // preserve id
        dto.setId(id);
        // simple save acts as upsert
        WorkstationDto saved = workstationService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/workstations/{id}")
    public ResponseEntity<Void> deleteWorkstation(@PathVariable Long id) {
        workstationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ModuleDto toModuleDto(Module module) {
        return new ModuleDto(
            module.getId(),
            module.getName(),
            module.getDescription(),
            module.getType()
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
