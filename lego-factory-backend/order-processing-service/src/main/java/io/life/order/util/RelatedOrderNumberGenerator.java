package io.life.order.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * RelatedOrderNumberGenerator
 * 
 * Generates order numbers that maintain relationships across the order chain.
 * All orders stemming from the same customer order share the same base number.
 * 
 * Format: PREFIX-BASENUMBER
 * 
 * Example for customer order CO-12345:
 * - Customer Order: CO-12345
 * - Warehouse Order: WO-12345
 * - Production Order: PO-12345
 * - Production Control Order: PCO-12345
 * - Assembly Control Order: ACO-12345
 * - Supply Order: SO-12345
 * - Injection Molding Order: IMO-12345
 * - Parts Pre-Production Order: PPO-12345
 * - Part Finishing Order: PFO-12345
 * - Gear Assembly Order: GAO-12345
 * - Motor Assembly Order: MAO-12345
 * - Final Assembly Order: FAO-12345
 */
@Component
public class RelatedOrderNumberGenerator {

    // Order type prefixes
    public static final String PREFIX_CUSTOMER_ORDER = "CO";
    public static final String PREFIX_WAREHOUSE_ORDER = "WO";
    public static final String PREFIX_PRODUCTION_ORDER = "PO";
    public static final String PREFIX_PRODUCTION_CONTROL = "PCO";
    public static final String PREFIX_ASSEMBLY_CONTROL = "ACO";
    public static final String PREFIX_SUPPLY_ORDER = "SO";
    public static final String PREFIX_INJECTION_MOLDING = "IMO";
    public static final String PREFIX_PART_PREPRODUCTION = "PPO";
    public static final String PREFIX_PART_FINISHING = "PFO";
    public static final String PREFIX_GEAR_ASSEMBLY = "GAO";
    public static final String PREFIX_MOTOR_ASSEMBLY = "MAO";
    public static final String PREFIX_FINAL_ASSEMBLY = "FAO";

    /**
     * Generate a new base customer order number.
     * This is the root of the order chain.
     * 
     * @return Customer order number (e.g., "CO-A1B2C3D4")
     */
    public String generateCustomerOrderNumber() {
        String baseNumber = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PREFIX_CUSTOMER_ORDER + "-" + baseNumber;
    }

    /**
     * Extract the base number from any order number.
     * Strips the prefix to get the numeric/alphanumeric identifier.
     * 
     * @param orderNumber Full order number (e.g., "CO-12345", "WO-12345")
     * @return Base number (e.g., "12345")
     */
    public String extractBaseNumber(String orderNumber) {
        if (orderNumber == null || !orderNumber.contains("-")) {
            return null;
        }
        // Remove prefix and return base number
        return orderNumber.substring(orderNumber.indexOf("-") + 1);
    }

    /**
     * Generate a related order number using the same base as the source order.
     * 
     * @param sourceOrderNumber The originating order number
     * @param targetPrefix The prefix for the new order type
     * @return Related order number with same base
     */
    public String generateRelatedOrderNumber(String sourceOrderNumber, String targetPrefix) {
        String baseNumber = extractBaseNumber(sourceOrderNumber);
        if (baseNumber == null) {
            // Fallback if source is invalid
            return targetPrefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return targetPrefix + "-" + baseNumber;
    }

    // Convenience methods for each order type

    public String generateWarehouseOrderNumber(String customerOrderNumber) {
        return generateRelatedOrderNumber(customerOrderNumber, PREFIX_WAREHOUSE_ORDER);
    }

    public String generateProductionOrderNumber(String sourceOrderNumber) {
        return generateRelatedOrderNumber(sourceOrderNumber, PREFIX_PRODUCTION_ORDER);
    }

    public String generateProductionControlOrderNumber(String productionOrderNumber) {
        return generateRelatedOrderNumber(productionOrderNumber, PREFIX_PRODUCTION_CONTROL);
    }

    public String generateAssemblyControlOrderNumber(String productionOrderNumber) {
        return generateRelatedOrderNumber(productionOrderNumber, PREFIX_ASSEMBLY_CONTROL);
    }

    public String generateSupplyOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_SUPPLY_ORDER);
    }

    public String generateInjectionMoldingOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_INJECTION_MOLDING);
    }

    public String generatePartPreProductionOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_PART_PREPRODUCTION);
    }

    public String generatePartFinishingOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_PART_FINISHING);
    }

    public String generateGearAssemblyOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_GEAR_ASSEMBLY);
    }

    public String generateMotorAssemblyOrderNumber(String controlOrderNumber) {
        return generateRelatedOrderNumber(controlOrderNumber, PREFIX_MOTOR_ASSEMBLY);
    }

    public String generateFinalAssemblyOrderNumber(String sourceOrderNumber) {
        return generateRelatedOrderNumber(sourceOrderNumber, PREFIX_FINAL_ASSEMBLY);
    }
}
