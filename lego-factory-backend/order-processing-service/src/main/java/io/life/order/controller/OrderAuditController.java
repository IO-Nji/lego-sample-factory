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
            @RequestParam String orderType,
            @RequestParam Long orderId) {
        return ResponseEntity.ok(auditService.find(orderType, orderId));
    }
}
