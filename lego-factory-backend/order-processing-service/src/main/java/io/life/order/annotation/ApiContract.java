package io.life.order.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for DTOs that define external API contracts.
 * 
 * This annotation indicates that the DTO is part of a published API
 * and changes require careful versioning and backward compatibility considerations.
 * 
 * <p>Purpose:</p>
 * <ul>
 *   <li>Identify DTOs used in cross-service communication</li>
 *   <li>Mark DTOs exposed to frontend clients</li>
 *   <li>Document API versioning requirements</li>
 *   <li>Enable automated API documentation generation</li>
 * </ul>
 * 
 * <p>Usage Example:</p>
 * <pre>{@code
 * @ApiContract(
 *     version = "v1",
 *     externalSource = "masterdata-service",
 *     description = "Product BOM module information"
 * )
 * public class ProductModuleDTO {
 *     // DTO fields...
 * }
 * }</pre>
 * 
 * <p>Breaking Changes:</p>
 * When making breaking changes to an @ApiContract DTO:
 * <ol>
 *   <li>Create new version (v2) of the DTO</li>
 *   <li>Support both versions for deprecation period (2 releases)</li>
 *   <li>Update API_CONTRACTS.md documentation</li>
 *   <li>Add migration guide for API consumers</li>
 * </ol>
 * 
 * @since 1.1.0 (Phase 1: API Contract Standardization)
 * @see com.fasterxml.jackson.annotation.JsonProperty for field name mapping
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiContract {
    
    /**
     * API version this DTO represents.
     * Follow semantic versioning: v1, v2, v3, etc.
     * 
     * @return API version identifier
     */
    String version() default "v1";
    
    /**
     * External system or service that consumes/produces this DTO.
     * 
     * Examples:
     * <ul>
     *   <li>"frontend" - React frontend application</li>
     *   <li>"masterdata-service" - Masterdata microservice</li>
     *   <li>"inventory-service" - Inventory microservice</li>
     *   <li>"external-api" - Third-party system</li>
     * </ul>
     * 
     * @return source/consumer identifier
     */
    String externalSource() default "";
    
    /**
     * Human-readable description of this API contract's purpose.
     * 
     * @return contract description
     */
    String description() default "";
    
    /**
     * Whether this contract is deprecated and scheduled for removal.
     * 
     * @return true if deprecated
     */
    boolean deprecated() default false;
    
    /**
     * Version in which this contract will be removed (if deprecated).
     * 
     * @return removal version (e.g., "v3.0.0")
     */
    String deprecatedSince() default "";
}
