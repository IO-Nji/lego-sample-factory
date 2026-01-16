package io.life.order.controller;

import io.life.order.entity.OrderAudit;
import io.life.order.service.OrderAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/audit")
public class OrderAuditController {
    private final OrderAuditService auditService;

    public OrderAuditController(OrderAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<OrderAudit>> list(
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) Long orderId) {
        if (orderType != null && orderId != null) {
            return ResponseEntity.ok(auditService.find(orderType, orderId));
        }
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<OrderAudit>> recent(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.findRecent(limit));
    }
    
    @PostMapping("/login")
    public ResponseEntity<OrderAudit> logLogin(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String userRole,
            @RequestHeader(value = "X-Authenticated-Username", required = false) String username) {
        
        OrderAudit audit = new OrderAudit();
        audit.setOrderType("SYSTEM");
        audit.setOrderId(0L); // No specific order for login events
        audit.setEventType("USER_LOGIN");
        audit.setDescription("User logged in");
        audit.setUserRole(userRole != null ? userRole : "UNKNOWN");
        
        if (userId != null) {
            try {
                audit.setUserId(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                // Leave userId as null if parsing fails
            }
        }
        
        OrderAudit saved = auditService.saveAudit(audit);
        return ResponseEntity.ok(saved);
    }
}
