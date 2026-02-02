package io.life.masterdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ApiContract Annotation
 * 
 * Marks DTOs that represent external API contracts requiring strict versioning
 * and backward compatibility guarantees.
 * 
 * This annotation identifies DTOs that are:
 * - Exposed to external clients (frontend, other services)
 * - Part of documented API contracts
 * - Subject to versioning policies
 * - Require breaking change notifications
 * 
 * Usage:
 * <pre>
 * {@literal @}ApiContract(
 *     version = "v1",
 *     externalSource = "order-service",
 *     description = "Product master data with BOM relationships"
 * )
 * public class ProductDto { ... }
 * </pre>
 * 
 * Breaking Change Protocol:
 * When modifying a DTO marked with @ApiContract:
 * 1. Support BOTH old and new field names for 2 release cycles
 * 2. Add @JsonProperty mappings for field renames
 * 3. Document changes in API_CONTRACTS.md
 * 4. Increment version number if breaking
 * 5. Set deprecated=true on old implementations
 * 
 * @see io.life.masterdata.annotation for centralized API contract documentation
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiContract {
    
    /**
     * API version this contract implements (e.g., "v1", "v2").
     */
    String version() default "v1";
    
    /**
     * External source consuming this API (e.g., "frontend", "order-service").
     */
    String externalSource();
    
    /**
     * Human-readable description of this API contract's purpose.
     */
    String description();
    
    /**
     * Whether this contract version is deprecated.
     */
    boolean deprecated() default false;
    
    /**
     * Version since when this contract has been deprecated.
     */
    String deprecatedSince() default "";
}
