package io.life.inventory.dto;

import io.life.inventory.annotation.ApiContract;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * StockAdjustmentRequest - Credit/debit stock operations.
 * 
 * API Contract: order-service calls POST /api/stock/adjust
 * CRITICAL: delta field is signed integer (positive=credit, negative=debit)
 * 
 * <p>Validation (Issue #3 Fix - Feb 4, 2026):</p>
 * - workstationId: Required, must be 1-9
 * - itemType: Required, must be PRODUCT, MODULE, or PART
 * - itemId: Required, must be positive
 * - delta: Required, -10000 to +10000 (non-zero)
 * - reasonCode: Required, max 50 chars
 */
@ApiContract(
    version = "v1",
    externalSource = "order-service",
    description = "Stock adjustment for credit/debit operations"
)
@Data
public class StockAdjustmentRequest {
    @NotNull(message = "Workstation ID is required")
    @Min(value = 1, message = "Workstation ID must be between 1 and 9")
    @Max(value = 9, message = "Workstation ID must be between 1 and 9")
    private Long workstationId;
    
    @NotNull(message = "Item type is required")
    @Pattern(regexp = "^(PRODUCT|MODULE|PART)$", message = "Item type must be PRODUCT, MODULE, or PART")
    private String itemType;
    
    @NotNull(message = "Item ID is required")
    @Positive(message = "Item ID must be positive")
    private Long itemId;
    
    @NotNull(message = "Delta (adjustment amount) is required")
    @Min(value = -10000, message = "Delta cannot be less than -10,000")
    @Max(value = 10000, message = "Delta cannot exceed 10,000")
    private Integer delta; // positive or negative
    
    @NotNull(message = "Reason code is required")
    @Size(max = 50, message = "Reason code cannot exceed 50 characters")
    private String reasonCode; // e.g. ADJUSTMENT, RECEIPT, CONSUMPTION
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
