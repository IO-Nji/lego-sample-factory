package io.life.masterdata.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found.
 * Typically used for products, modules, parts, and BOM entries.
 */
public class ResourceNotFoundException extends MasterdataException {
    
    public ResourceNotFoundException(String message) {
        super(message, "MASTERDATA_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            "MASTERDATA_NOT_FOUND",
            Map.of(
                "resourceName", resourceName,
                "fieldName", fieldName,
                "fieldValue", fieldValue != null ? fieldValue.toString() : "null"
            )
        );
    }
}
