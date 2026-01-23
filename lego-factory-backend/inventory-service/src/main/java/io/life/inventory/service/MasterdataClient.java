package io.life.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client service for fetching item names from masterdata-service.
 * Enriches inventory records with actual product/module/part names.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterdataClient {

    private final RestTemplate restTemplate;
    private static final String MASTERDATA_BASE_URL = "http://masterdata-service:8013/api/masterdata";

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

            String url = MASTERDATA_BASE_URL + endpoint + "/" + itemId;
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
}
