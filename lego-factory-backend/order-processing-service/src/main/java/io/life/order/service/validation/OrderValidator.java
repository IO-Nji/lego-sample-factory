package io.life.order.service.validation;

import io.life.order.entity.CustomerOrder;
import io.life.order.entity.OrderItem;
import io.life.order.entity.WarehouseOrder;
import io.life.order.entity.WarehouseOrderItem;
import io.life.order.exception.OrderProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OrderValidator - Centralized order validation logic
 * 
 * Provides fluent validation API that collects ALL validation errors before throwing.
 * Extracted from various services to provide single-responsibility validation.
 * 
 * Usage:
 * <pre>
 * orderValidator.validateForFulfillment(order);  // Throws if invalid
 * </pre>
 * 
 * @see CustomerOrderService - uses for customer order validation
 * @see WarehouseOrderService - uses for warehouse order validation
 */
@Component
@Slf4j
public class OrderValidator {

    // ========================
    // CUSTOMER ORDER VALIDATION
    // ========================

    /**
     * Validate a customer order is ready for fulfillment.
     * Collects ALL validation errors before throwing exception.
     * 
     * @param order The customer order to validate
     * @throws OrderProcessingException if validation fails with all errors listed
     */
    public void validateForFulfillment(CustomerOrder order) {
        List<String> errors = new ArrayList<>();

        if (order == null) {
            throw new OrderProcessingException("Order cannot be null")
                    .addDetail("operation", "validateForFulfillment");
        }

        // Basic field validation
        if (order.getId() == null) {
            errors.add("Order ID is required");
        }

        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            errors.add("Order number is required");
        }

        // Status validation
        if (!"CONFIRMED".equals(order.getStatus())) {
            errors.add(String.format("Order status must be CONFIRMED, current: %s", order.getStatus()));
        }

        // Items validation
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            errors.add("Order must contain at least one item");
        } else {
            for (int i = 0; i < order.getOrderItems().size(); i++) {
                OrderItem item = order.getOrderItems().get(i);
                validateOrderItem(item, i, errors);
            }
        }

        // Workstation validation
        if (order.getWorkstationId() == null) {
            errors.add("Workstation ID is required");
        }

        throwIfErrors(errors, order.getOrderNumber(), "CustomerOrder");
    }

    /**
     * Validate a customer order for confirmation (before it's been confirmed).
     */
    public void validateForConfirmation(CustomerOrder order) {
        List<String> errors = new ArrayList<>();

        if (order == null) {
            throw new OrderProcessingException("Order cannot be null")
                    .addDetail("operation", "validateForConfirmation");
        }

        // Status must be PENDING
        if (!"PENDING".equals(order.getStatus())) {
            errors.add(String.format("Order status must be PENDING for confirmation, current: %s", 
                    order.getStatus()));
        }

        // Items validation
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            errors.add("Order must contain at least one item");
        } else {
            for (int i = 0; i < order.getOrderItems().size(); i++) {
                OrderItem item = order.getOrderItems().get(i);
                validateOrderItem(item, i, errors);
            }
        }

        throwIfErrors(errors, order.getOrderNumber(), "CustomerOrder");
    }

    /**
     * Validate a single order item.
     */
    private void validateOrderItem(OrderItem item, int index, List<String> errors) {
        if (item == null) {
            errors.add(String.format("Item %d: cannot be null", index));
            return;
        }

        if (item.getItemId() == null) {
            errors.add(String.format("Item %d: itemId is required", index));
        }

        if (item.getItemType() == null || item.getItemType().isBlank()) {
            errors.add(String.format("Item %d: itemType is required", index));
        } else if (!isValidItemType(item.getItemType())) {
            errors.add(String.format("Item %d: invalid itemType '%s', must be PRODUCT, MODULE, or PART", 
                    index, item.getItemType()));
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            errors.add(String.format("Item %d: quantity must be positive, got: %s", 
                    index, item.getQuantity()));
        }
    }

    // ========================
    // WAREHOUSE ORDER VALIDATION
    // ========================

    /**
     * Validate a warehouse order for fulfillment.
     */
    public void validateWarehouseOrderForFulfillment(WarehouseOrder order) {
        List<String> errors = new ArrayList<>();

        if (order == null) {
            throw new OrderProcessingException("Warehouse order cannot be null")
                    .addDetail("operation", "validateWarehouseOrderForFulfillment");
        }

        if (order.getId() == null) {
            errors.add("Warehouse order ID is required");
        }

        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            errors.add("Warehouse order number is required");
        }

        // Status validation - can be CONFIRMED or MODULES_READY for fulfillment
        String status = order.getStatus();
        if (!"CONFIRMED".equals(status) && !"MODULES_READY".equals(status)) {
            errors.add(String.format("Warehouse order status must be CONFIRMED or MODULES_READY, current: %s", status));
        }

        // Items validation
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            errors.add("Warehouse order must contain at least one item");
        } else {
            for (int i = 0; i < order.getOrderItems().size(); i++) {
                WarehouseOrderItem item = order.getOrderItems().get(i);
                validateWarehouseOrderItem(item, i, errors);
            }
        }

        throwIfErrors(errors, order.getOrderNumber(), "WarehouseOrder");
    }

    /**
     * Validate a warehouse order for confirmation.
     */
    public void validateWarehouseOrderForConfirmation(WarehouseOrder order) {
        List<String> errors = new ArrayList<>();

        if (order == null) {
            throw new OrderProcessingException("Warehouse order cannot be null")
                    .addDetail("operation", "validateWarehouseOrderForConfirmation");
        }

        if (!"PENDING".equals(order.getStatus())) {
            errors.add(String.format("Warehouse order status must be PENDING for confirmation, current: %s", 
                    order.getStatus()));
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            errors.add("Warehouse order must contain at least one item");
        }

        throwIfErrors(errors, order.getOrderNumber(), "WarehouseOrder");
    }

    /**
     * Validate a single warehouse order item.
     */
    private void validateWarehouseOrderItem(WarehouseOrderItem item, int index, List<String> errors) {
        if (item == null) {
            errors.add(String.format("Warehouse item %d: cannot be null", index));
            return;
        }

        if (item.getItemId() == null) {
            errors.add(String.format("Warehouse item %d: itemId is required", index));
        }

        if (item.getItemType() == null || item.getItemType().isBlank()) {
            errors.add(String.format("Warehouse item %d: itemType is required", index));
        }

        if (item.getRequestedQuantity() == null || item.getRequestedQuantity() <= 0) {
            errors.add(String.format("Warehouse item %d: requestedQuantity must be positive", index));
        }

        // productId is optional but should be valid if present
        // (productId links to the original product for Final Assembly)
    }

    // ========================
    // BOM VALIDATION
    // ========================

    /**
     * Validate BOM conversion result contains valid data.
     */
    public void validateBomResult(Map<Long, Integer> moduleRequirements, Long productId) {
        List<String> errors = new ArrayList<>();

        if (moduleRequirements == null) {
            errors.add("BOM result is null for product " + productId);
        } else if (moduleRequirements.isEmpty()) {
            errors.add("BOM result is empty for product " + productId + " (no modules defined)");
        } else {
            for (Map.Entry<Long, Integer> entry : moduleRequirements.entrySet()) {
                if (entry.getKey() == null) {
                    errors.add("BOM contains null moduleId for product " + productId);
                }
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    errors.add(String.format("BOM contains invalid quantity %s for module %s (product %s)",
                            entry.getValue(), entry.getKey(), productId));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new OrderProcessingException("BOM validation failed: " + String.join(", ", errors))
                    .addDetail("productId", productId)
                    .addDetail("validationErrors", errors);
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Check if an item type is valid.
     */
    private boolean isValidItemType(String itemType) {
        return "PRODUCT".equalsIgnoreCase(itemType) ||
               "MODULE".equalsIgnoreCase(itemType) ||
               "PART".equalsIgnoreCase(itemType);
    }

    /**
     * Throw exception if errors list is not empty.
     */
    private void throwIfErrors(List<String> errors, String orderNumber, String orderType) {
        if (!errors.isEmpty()) {
            log.warn("Validation failed for {} {}: {}", orderType, orderNumber, errors);
            throw new OrderProcessingException(
                    orderType + " validation failed: " + String.join(", ", errors))
                    .addDetail("orderNumber", orderNumber)
                    .addDetail("orderType", orderType)
                    .addDetail("validationErrors", errors)
                    .addDetail("errorCount", errors.size());
        }
    }

    /**
     * Quick check if an order is valid without throwing.
     * 
     * @return true if order passes all validation rules
     */
    public boolean isValidForFulfillment(CustomerOrder order) {
        try {
            validateForFulfillment(order);
            return true;
        } catch (OrderProcessingException e) {
            return false;
        }
    }

    /**
     * Quick check if a warehouse order is valid without throwing.
     */
    public boolean isValidWarehouseOrderForFulfillment(WarehouseOrder order) {
        try {
            validateWarehouseOrderForFulfillment(order);
            return true;
        } catch (OrderProcessingException e) {
            return false;
        }
    }
}
