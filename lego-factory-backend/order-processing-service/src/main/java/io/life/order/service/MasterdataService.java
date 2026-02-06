package io.life.order.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.life.order.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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
 * 
 * <p>Caching Strategy (Phase 3 - Feb 4, 2026):
 * <ul>
 *   <li>Product modules (BOM): Cached for 1 hour - rarely changes</li>
 *   <li>Module parts (BOM): Cached for 1 hour - rarely changes</li>
 *   <li>Item names: Cached for 10 minutes - static reference data</li>
 * </ul>
 * 
 * <p>Note: Uses @Lazy self-injection to enable @Cacheable on internal method calls.
 */
@Service
@Slf4j
public class MasterdataService {

    private final RestTemplate restTemplate;
    
    /** Self-reference for enabling caching on internal calls */
    private MasterdataService self;

    @Value("${masterdata.service.url:${MASTERDATA_SERVICE_URL:http://masterdata-service:8013}}")
    private String masterdataServiceUrl;
    
    public MasterdataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Autowired
    public void setSelf(@Lazy MasterdataService self) {
        this.self = self;
    }

    /**
     * Fetch item name from masterdata-service based on item type and ID.
     * Results are cached to reduce inter-service calls.
     * 
     * @param itemType "PRODUCT", "MODULE", or "PART"
     * @param itemId The item's ID
     * @return The item's name, or a fallback string if not found
     */
    @Cacheable(value = CacheConfig.CACHE_ITEM_NAMES, key = "#itemType + ':' + #itemId", 
               unless = "#result == null || #result.contains('#')")
    @CircuitBreaker(name = "masterdataService", fallbackMethod = "getItemNameFallback")
    @Retry(name = "masterdataService")
    public String getItemName(String itemType, Long itemId) {
        log.debug("Cache MISS: Fetching item name for {} #{}", itemType, itemId);
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
     * Results are cached for 1 hour since BOM data rarely changes.
     * 
     * @param productId The product ID
     * @return List of ProductModuleDTO containing module IDs and quantities
     */
    @Cacheable(value = CacheConfig.CACHE_PRODUCT_MODULES, key = "#productId", 
               unless = "#result == null || #result.isEmpty()")
    @CircuitBreaker(name = "masterdataService", fallbackMethod = "getProductModulesFallback")
    @Retry(name = "masterdataService")
    public List<ProductModuleDTO> getProductModules(Long productId) {
        log.debug("Cache MISS: Fetching product modules for product {}", productId);
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
     * Uses self-reference to enable caching on getProductModules call.
     * 
     * @param productId Product ID
     * @param orderQuantity Quantity of products ordered
     * @return Map of module ID to total required quantity
     */
    public Map<Long, Integer> getModuleRequirementsForProduct(Long productId, Integer orderQuantity) {
        // Use self-reference to enable @Cacheable on internal call
        List<ProductModuleDTO> productModules = self.getProductModules(productId);
        
        Map<Long, Integer> requirements = new HashMap<>();
        for (ProductModuleDTO pm : productModules) {
            int totalRequired = pm.getQuantity() * orderQuantity;
            requirements.merge(pm.getModuleId(), totalRequired, Integer::sum);
        }
        
        return requirements;
    }

    /**
     * Fetch module parts (bill of materials) for a module.
     * Results are cached for 1 hour since BOM data rarely changes.
     * 
     * @param moduleId The module ID
     * @return List of ModulePartDTO containing part IDs and quantities
     */
    @Cacheable(value = CacheConfig.CACHE_MODULE_PARTS, key = "#moduleId", 
               unless = "#result == null || #result.isEmpty()")
    @CircuitBreaker(name = "masterdataService", fallbackMethod = "getModulePartsFallback")
    @Retry(name = "masterdataService")
    public List<ModulePartDTO> getModuleParts(Long moduleId) {
        log.debug("Cache MISS: Fetching module parts for module {}", moduleId);
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
     * Uses self-reference to enable caching on getModuleParts call.
     * 
     * @param moduleId Module ID
     * @param moduleQuantity Quantity of modules to build
     * @return Map of part ID to total required quantity
     */
    public Map<Long, Integer> getPartRequirementsForModule(Long moduleId, Integer moduleQuantity) {
        // Use self-reference to enable @Cacheable on internal call
        List<ModulePartDTO> moduleParts = self.getModuleParts(moduleId);
        
        Map<Long, Integer> requirements = new HashMap<>();
        for (ModulePartDTO mp : moduleParts) {
            int totalRequired = mp.getQuantity() * moduleQuantity;
            requirements.merge(mp.getPartId(), totalRequired, Integer::sum);
        }
        
        return requirements;
    }

    // ================================================
    // Circuit Breaker Fallback Methods
    // ================================================

    /**
     * Fallback for getItemName when masterdata service is unavailable.
     * Returns a generic label with the item type and ID.
     */
    @SuppressWarnings("unused")
    private String getItemNameFallback(String itemType, Long itemId, Throwable t) {
        log.warn("Circuit breaker fallback: getItemName failed for {} #{}. Reason: {}", 
                itemType, itemId, t.getMessage());
        return String.format("%s #%d (name unavailable)", itemType, itemId);
    }

    /**
     * Fallback for getProductModules when masterdata service is unavailable.
     * Returns empty list - caller should handle accordingly.
     */
    @SuppressWarnings("unused")
    private List<ProductModuleDTO> getProductModulesFallback(Long productId, Throwable t) {
        log.error("Circuit breaker fallback: getProductModules failed for product {}. Reason: {}", 
                productId, t.getMessage());
        return Collections.emptyList();
    }

    /**
     * Fallback for getModuleParts when masterdata service is unavailable.
     * Returns empty list - caller should handle accordingly.
     */
    @SuppressWarnings("unused")
    private List<ModulePartDTO> getModulePartsFallback(Long moduleId, Throwable t) {
        log.error("Circuit breaker fallback: getModuleParts failed for module {}. Reason: {}", 
                moduleId, t.getMessage());
        return Collections.emptyList();
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
     * Handles both old format (moduleId) and new format (componentId).
     */
    public static class ProductModuleDTO implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long id;
        private Long productId;
        private Long moduleId;
        private Long componentId;  // New format from masterdata-service
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
            // Return componentId if moduleId is null (new API format)
            return moduleId != null ? moduleId : componentId;
        }

        public void setModuleId(Long moduleId) {
            this.moduleId = moduleId;
        }

        public Long getComponentId() {
            return componentId;
        }

        public void setComponentId(Long componentId) {
            this.componentId = componentId;
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
    /**
     * DTO matching BomEntryDTO structure from masterdata-service.
     * Used for deserializing module BOM responses.
     * 
     * Fields match masterdata-service/dto/BomEntryDTO:
     * - componentId: The part/module ID
     * - componentName: The part/module name
     * - componentType: "PART" or "MODULE"
     * - quantity: Quantity required per parent item
     * - partId: Legacy field, mapped to componentId
     */
    public static class ModulePartDTO implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long componentId;
        private String componentName;
        private String componentType;
        private Integer quantity;
        private Long partId;  // Legacy field from old API responses

        public ModulePartDTO() {}

        public Long getComponentId() {
            return componentId;
        }

        public void setComponentId(Long componentId) {
            this.componentId = componentId;
        }

        public String getComponentName() {
            return componentName;
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public String getComponentType() {
            return componentType;
        }

        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        /**
         * Sets the legacy partId field. Maps to componentId.
         */
        public void setPartId(Long partId) {
            this.partId = partId;
            // Also set componentId if not already set
            if (this.componentId == null && partId != null) {
                this.componentId = partId;
            }
        }
        
        /**
         * Convenience method for backward compatibility.
         * Returns componentId as partId for supply order logic.
         * If componentId is null but partId was set, returns partId.
         */
        public Long getPartId() {
            return componentId != null ? componentId : partId;
        }
    }
}
