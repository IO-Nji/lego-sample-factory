package io.life.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MasterdataClient
 * 
 * Centralized client for all masterdata-service API calls.
 * Provides access to products, modules, parts, and BOM (Bill of Materials) lookups.
 * 
 * BOM HIERARCHY:
 * Product → Modules → Parts
 * 
 * Example:
 * - Product "LEGO Model Car" requires:
 *   - Gear Module (which requires gear parts)
 *   - Motor Module (which requires motor parts)
 */
@Component
@Slf4j
public class MasterdataClient {

    private final RestTemplate restTemplate;
    
    @Value("${masterdata.service.url:http://masterdata-service:8013}")
    private String masterdataServiceUrl;

    public MasterdataClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ========================
    // PRODUCT METHODS
    // ========================

    /**
     * Get all products.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllProducts() {
        try {
            String url = masterdataServiceUrl + "/api/products";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch products: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get product by ID.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getProductById(Long productId) {
        try {
            String url = masterdataServiceUrl + "/api/products/" + productId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to fetch product {}: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get product name by ID.
     */
    public String getProductName(Long productId) {
        return getProductById(productId)
                .map(p -> (String) p.get("name"))
                .orElse("Unknown Product " + productId);
    }

    // ========================
    // MODULE METHODS
    // ========================

    /**
     * Get all modules.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllModules() {
        try {
            String url = masterdataServiceUrl + "/api/modules";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch modules: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get module by ID.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getModuleById(Long moduleId) {
        try {
            String url = masterdataServiceUrl + "/api/modules/" + moduleId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to fetch module {}: {}", moduleId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get module name by ID.
     */
    public String getModuleName(Long moduleId) {
        return getModuleById(moduleId)
                .map(m -> (String) m.get("name"))
                .orElse("Unknown Module " + moduleId);
    }

    // ========================
    // PART METHODS
    // ========================

    /**
     * Get all parts.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllParts() {
        try {
            String url = masterdataServiceUrl + "/api/parts";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch parts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get part by ID.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getPartById(Long partId) {
        try {
            String url = masterdataServiceUrl + "/api/parts/" + partId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to fetch part {}: {}", partId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get part name by ID.
     */
    public String getPartName(Long partId) {
        return getPartById(partId)
                .map(p -> (String) p.get("name"))
                .orElse("Unknown Part " + partId);
    }

    // ========================
    // BOM (BILL OF MATERIALS) METHODS
    // ========================

    /**
     * Get modules required for a product.
     * Returns list of module requirements with quantities.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getModulesForProduct(Long productId) {
        try {
            String url = masterdataServiceUrl + "/api/products/" + productId + "/modules";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch modules for product {}: {}", productId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get parts required for a module.
     * Returns list of part requirements with quantities.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPartsForModule(Long moduleId) {
        try {
            String url = masterdataServiceUrl + "/api/modules/" + moduleId + "/parts";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch parts for module {}: {}", moduleId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get full BOM (all parts required) for a product.
     * Aggregates parts from all required modules.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFullBomForProduct(Long productId) {
        try {
            String url = masterdataServiceUrl + "/api/products/" + productId + "/bom";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch BOM for product {}: {}", productId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ========================
    // WORKSTATION METHODS
    // ========================

    /**
     * Get all workstations.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllWorkstations() {
        try {
            String url = masterdataServiceUrl + "/api/workstations";
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch workstations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get workstation by ID.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getWorkstationById(Long workstationId) {
        try {
            String url = masterdataServiceUrl + "/api/workstations/" + workstationId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Failed to fetch workstation {}: {}", workstationId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get workstation name by ID.
     */
    public String getWorkstationName(Long workstationId) {
        return getWorkstationById(workstationId)
                .map(w -> (String) w.get("name"))
                .orElse("Unknown Workstation " + workstationId);
    }

    // ========================
    // EXCEPTION CLASS
    // ========================

    public static class MasterdataOperationException extends RuntimeException {
        public MasterdataOperationException(String message, Throwable cause) {
            super(message, cause);
        }

        public MasterdataOperationException(String message) {
            super(message);
        }
    }
}
