package io.life.masterdata.controller;

import io.life.masterdata.dto.ProductVariantDto;
import io.life.masterdata.entity.Module;
import io.life.masterdata.entity.ModulePart;
import io.life.masterdata.entity.Part;
import io.life.masterdata.entity.ProductModule;
import io.life.masterdata.entity.ProductVariant;
import io.life.masterdata.service.ModulePartService;
import io.life.masterdata.service.ModuleService;
import io.life.masterdata.service.PartService;
import io.life.masterdata.service.ProductModuleService;
import io.life.masterdata.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for ProductVariant management.
 * Allows admins to view, create, update, and delete product variants.
 */
@RestController
@RequestMapping("/api/masterdata/product-variants")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class ProductVariantController {

    private final ProductVariantService productVariantService;
    private final ProductModuleService productModuleService;
    private final ModuleService moduleService;
    private final ModulePartService modulePartService;
    private final PartService partService;

    /**
     * GET /api/masterdata/product-variants
     * Get all product variants
     */
    @GetMapping
    public ResponseEntity<List<ProductVariantDto>> getAllProductVariants() {
        log.debug("Fetching all product variants");
        List<ProductVariantDto> variants = productVariantService.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(variants);
    }

    /**
     * GET /api/masterdata/product-variants/{id}
     * Get a specific product variant by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductVariantDto> getProductVariantById(@PathVariable Long id) {
        log.debug("Fetching product variant with ID: {}", id);
        return productVariantService.findById(id)
                .map(variant -> ResponseEntity.ok(convertToDto(variant)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/masterdata/product-variants
     * Create a new product variant
     * Request body: { "name", "description", "price", "estimatedTimeMinutes" }
     */
    @PostMapping
    public ResponseEntity<ProductVariantDto> createProductVariant(@RequestBody ProductVariantDto dto) {
        log.info("Creating new product variant: {}", dto.getName());
        
        ProductVariant variant = new ProductVariant();
        variant.setName(dto.getName());
        variant.setDescription(dto.getDescription());
        variant.setPrice(dto.getPrice());
        variant.setEstimatedTimeMinutes(dto.getEstimatedTimeMinutes());

        try {
            ProductVariant saved = productVariantService.save(variant);
            log.info("Product variant created successfully with ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
        } catch (Exception e) {
            log.error("Failed to create product variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * PUT /api/masterdata/product-variants/{id}
     * Update an existing product variant
     * Request body: { "name", "description", "price", "estimatedTimeMinutes" }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductVariantDto> updateProductVariant(
            @PathVariable Long id,
            @RequestBody ProductVariantDto dto) {
        log.info("Updating product variant with ID: {}", id);

        var optionalVariant = productVariantService.findById(id);
        if (optionalVariant.isEmpty()) {
            log.warn("Product variant not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        ProductVariant variant = optionalVariant.get();
        variant.setName(dto.getName());
        variant.setDescription(dto.getDescription());
        variant.setPrice(dto.getPrice());
        variant.setEstimatedTimeMinutes(dto.getEstimatedTimeMinutes());

        try {
            ProductVariant updated = productVariantService.save(variant);
            log.info("Product variant updated successfully with ID: {}", id);
            return ResponseEntity.ok(convertToDto(updated));
        } catch (Exception e) {
            log.error("Failed to update product variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * DELETE /api/masterdata/product-variants/{id}
     * Delete a product variant
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductVariant(@PathVariable Long id) {
        log.info("Deleting product variant with ID: {}", id);

        if (productVariantService.findById(id).isPresent()) {
            try {
                productVariantService.deleteById(id);
                log.info("Product variant deleted successfully with ID: {}", id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Failed to delete product variant: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } else {
            log.warn("Product variant not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/masterdata/product-variants/{id}/composition
     * Get the complete composition (modules and parts) for a specific product
     */
    @GetMapping("/{id}/composition")
    public ResponseEntity<Map<String, Object>> getProductComposition(@PathVariable Long id) {
        log.debug("Fetching composition for product variant ID: {}", id);
        
        Optional<ProductVariant> productOpt = productVariantService.findById(id);
        if (productOpt.isEmpty()) {
            log.warn("Product variant not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        // Get all modules for this product
        List<ProductModule> productModules = productModuleService.findByProductVariantId(id);
        
        List<Map<String, Object>> modulesWithParts = new ArrayList<>();
        
        for (ProductModule pm : productModules) {
            Optional<Module> moduleOpt = moduleService.findById(pm.getModuleId());
            if (moduleOpt.isEmpty()) continue;
            
            Module module = moduleOpt.get();
            
            // Get all parts for this module
            List<ModulePart> moduleParts = modulePartService.findByModuleId(module.getId());
            
            List<Map<String, Object>> partsDetails = new ArrayList<>();
            for (ModulePart mp : moduleParts) {
                Optional<Part> partOpt = partService.findById(mp.getPartId());
                if (partOpt.isEmpty()) continue;
                
                Part part = partOpt.get();
                Map<String, Object> partDetail = new HashMap<>();
                partDetail.put("id", part.getId());
                partDetail.put("name", part.getName());
                partDetail.put("description", part.getDescription());
                partDetail.put("category", part.getCategory());
                partDetail.put("unitCost", part.getUnitCost());
                partDetail.put("quantity", mp.getQuantity());
                partsDetails.add(partDetail);
            }
            
            Map<String, Object> moduleDetail = new HashMap<>();
            moduleDetail.put("id", module.getId());
            moduleDetail.put("name", module.getName());
            moduleDetail.put("description", module.getDescription());
            moduleDetail.put("type", module.getType());
            moduleDetail.put("quantity", pm.getQuantity());
            moduleDetail.put("parts", partsDetails);
            
            modulesWithParts.add(moduleDetail);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("product", convertToDto(productOpt.get()));
        response.put("modules", modulesWithParts);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to convert ProductVariant entity to DTO
     */
    private ProductVariantDto convertToDto(ProductVariant variant) {
        return new ProductVariantDto(
                variant.getId(),
                variant.getName(),
                variant.getDescription(),
                variant.getPrice(),
                variant.getEstimatedTimeMinutes()
        );
    }
}
