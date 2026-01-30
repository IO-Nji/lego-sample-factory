package io.life.masterdata.controller;

import io.life.masterdata.dto.ProductDto;
import io.life.masterdata.entity.Module;
import io.life.masterdata.entity.ModulePart;
import io.life.masterdata.entity.Part;
import io.life.masterdata.entity.ProductModule;
import io.life.masterdata.entity.Product;
import io.life.masterdata.service.ModulePartService;
import io.life.masterdata.service.ModuleService;
import io.life.masterdata.service.PartService;
import io.life.masterdata.service.ProductModuleService;
import io.life.masterdata.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Product management.
 * Allows admins to view, create, update, and delete products.
 */
@RestController
@RequestMapping("/api/masterdata/products")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product catalog and BOM (Bill of Materials) management")
public class ProductController {

    private final ProductService productService;
    private final ProductModuleService productModuleService;
    private final ModuleService moduleService;
    private final ModulePartService modulePartService;
    private final PartService partService;

    @Operation(summary = "Get all products", description = "Retrieve all products in the catalog")
    @ApiResponse(responseCode = "200", description = "List of products")
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        log.debug("Fetching all products");
        List<ProductDto> products = productService.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("Fetching product with ID: {}", id);
        return productService.findById(id)
                .map(product -> ResponseEntity.ok(convertToDto(product)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create product", description = "Create a new product in the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto dto) {
        log.info("Creating new product: {}", dto.getName());
        
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setEstimatedTimeMinutes(dto.getEstimatedTimeMinutes());

        try {
            Product saved = productService.save(product);
            log.info("Product created successfully with ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
        } catch (Exception e) {
            log.error("Failed to create product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Update product", description = "Update an existing product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @RequestBody ProductDto dto) {
        log.info("Updating product with ID: {}", id);

        var optionalProduct = productService.findById(id);
        if (optionalProduct.isEmpty()) {
            log.warn("Product not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        Product product = optionalProduct.get();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setEstimatedTimeMinutes(dto.getEstimatedTimeMinutes());

        try {
            Product updated = productService.save(product);
            log.info("Product updated successfully with ID: {}", id);
            return ResponseEntity.ok(convertToDto(updated));
        } catch (Exception e) {
            log.error("Failed to update product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Delete product", description = "Delete a product from the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.info("Deleting product with ID: {}", id);

        if (productService.findById(id).isPresent()) {
            try {
                productService.deleteById(id);
                log.info("Product deleted successfully with ID: {}", id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Failed to delete product: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } else {
            log.warn("Product not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get product modules (BOM)", 
               description = "Get all modules with quantities required to build a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of product-module relationships"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/modules")
    public ResponseEntity<List<ProductModule>> getProductModules(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("Fetching modules for product ID: {}", id);
        
        Optional<Product> productOpt = productService.findById(id);
        if (productOpt.isEmpty()) {
            log.warn("Product not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        List<ProductModule> productModules = productModuleService.findByProductId(id);
        log.debug("Found {} modules for product ID: {}", productModules.size(), id);
        
        return ResponseEntity.ok(productModules);
    }

    @Operation(summary = "Get product composition (full BOM)", 
               description = "Get the complete Bill of Materials including all modules and their parts with quantities")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Full product composition with nested modules and parts"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/composition")
    public ResponseEntity<Map<String, Object>> getProductComposition(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("Fetching composition for product ID: {}", id);
        
        Optional<Product> productOpt = productService.findById(id);
        if (productOpt.isEmpty()) {
            log.warn("Product not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        // Get all modules for this product
        List<ProductModule> productModules = productModuleService.findByProductId(id);
        
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
     * Helper method to convert Product entity to DTO
     */
    private ProductDto convertToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getEstimatedTimeMinutes()
        );
    }
}
