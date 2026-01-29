package io.life.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client service for fetching item names and compositions from masterdata-service.
 * Used to enrich order items with actual product/module/part names and resolve module requirements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterdataService {

    private final RestTemplate restTemplate;

    @Value("${masterdata.service.url:${MASTERDATA_SERVICE_URL:http://masterdata-service:8013}}")
    private String masterdataServiceUrl;

    /**
     * Fetch item name from masterdata-service based on item type and ID.
     * 
     * @param itemType "PRODUCT", "MODULE", or "PART"
     * @param itemId The item's ID
     * @return The item's name, or a fallback string if not found
     */
    public String getItemName(String itemType, Long itemId) {
        try {
            String endpoint = getEndpointForType(itemType);
            if (endpoint == null) {
                log.warn("Unknown item type: {}", itemType);
                return String.format("%s #%d", itemType, itemId);
            }

            String url = masterdataServiceUrl + "/api/masterdata" + endpoint + "/" + itemId;
            ItemNameResponse response = restTemplate.getForObject(url, ItemNameResponse.class);
            
            if (response != null && response.getName() != null) {
                return response.getName();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch name for {} #{}: {}", itemType, itemId, e.getMessage());
        }

        // Fallback to generic label
        return String.format("%s #%d", itemType, itemId);
    }

    private String getEndpointForType(String itemType) {
        return switch (itemType.toUpperCase()) {
            case "PRODUCT" -> "/products";
            case "MODULE" -> "/modules";
            case "PART" -> "/parts";
            default -> null;
        };
    }

    /**
     * Fetch product modules (bill of materials) for a product.
     * Returns a list of modules with their required quantities.
     * 
     * @param productId The product ID
     * @return List of ProductModuleDTO containing module IDs and quantities
     */
    public List<ProductModuleDTO> getProductModules(Long productId) {
        try {
            String url = masterdataServiceUrl + "/api/masterdata/products/" + productId + "/modules";
            log.debug("Fetching product modules from: {}", url);
            
            ResponseEntity<List<ProductModuleDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductModuleDTO>>() {}
            );
            
            List<ProductModuleDTO> modules = response.getBody();
            if (modules != null && !modules.isEmpty()) {
                log.debug("Found {} modules for product {}", modules.size(), productId);
                return modules;
            }
        } catch (Exception e) {
            log.error("Failed to fetch product modules for product {}: {}", productId, e.getMessage());
        }
        
        return Collections.emptyList();
    }

    /**
     * Convert order items (products with quantities) to aggregated module requirements.
     * For each product, fetches its module composition and multiplies by order quantity.
     * 
     * @param productId Product ID
     * @param orderQuantity Quantity of products ordered
     * @return Map of module ID to total required quantity
     */
    public Map<Long, Integer> getModuleRequirementsForProduct(Long productId, Integer orderQuantity) {
        List<ProductModuleDTO> productModules = getProductModules(productId);
        
        Map<Long, Integer> requirements = new HashMap<>();
        for (ProductModuleDTO pm : productModules) {
            int totalRequired = pm.getQuantity() * orderQuantity;
            requirements.merge(pm.getModuleId(), totalRequired, Integer::sum);
        }
        
        return requirements;
    }

    /**
     * Fetch module parts (bill of materials) for a module.
     * Returns a list of parts with their required quantities.
     * 
     * @param moduleId The module ID
     * @return List of ModulePartDTO containing part IDs and quantities
     */
    public List<ModulePartDTO> getModuleParts(Long moduleId) {
        try {
            String url = masterdataServiceUrl + "/api/masterdata/modules/" + moduleId + "/parts";
            log.debug("Fetching module parts from: {}", url);
            
            ResponseEntity<List<ModulePartDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ModulePartDTO>>() {}
            );
            
            List<ModulePartDTO> parts = response.getBody();
            if (parts != null && !parts.isEmpty()) {
                log.debug("Found {} parts for module {}", parts.size(), moduleId);
                return parts;
            }
        } catch (Exception e) {
            log.error("Failed to fetch module parts for module {}: {}", moduleId, e.getMessage());
        }
        
        return Collections.emptyList();
    }

    /**
     * Get part requirements for a module with given quantity.
     * 
     * @param moduleId Module ID
     * @param moduleQuantity Quantity of modules to build
     * @return Map of part ID to total required quantity
     */
    public Map<Long, Integer> getPartRequirementsForModule(Long moduleId, Integer moduleQuantity) {
        List<ModulePartDTO> moduleParts = getModuleParts(moduleId);
        
        Map<Long, Integer> requirements = new HashMap<>();
        for (ModulePartDTO mp : moduleParts) {
            int totalRequired = mp.getQuantity() * moduleQuantity;
            requirements.merge(mp.getPartId(), totalRequired, Integer::sum);
        }
        
        return requirements;
    }

    /**
     * Simple DTO to extract 'name' field from masterdata-service responses.
     */
    public static class ItemNameResponse {
        private Long id;
        private String name;

        public ItemNameResponse() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * DTO for ProductModule data from masterdata-service.
     */
    public static class ProductModuleDTO {
        private Long id;
        private Long productId;
        private Long moduleId;
        private Integer quantity;

        public ProductModuleDTO() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getModuleId() {
            return moduleId;
        }

        public void setModuleId(Long moduleId) {
            this.moduleId = moduleId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * DTO for ModulePart data from masterdata-service.
     */
    public static class ModulePartDTO {
        private Long id;
        private Long moduleId;
        private Long partId;
        private Integer quantity;

        public ModulePartDTO() {}

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getModuleId() {
            return moduleId;
        }

        public void setModuleId(Long moduleId) {
            this.moduleId = moduleId;
        }

        public Long getPartId() {
            return partId;
        }

        public void setPartId(Long partId) {
            this.partId = partId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
