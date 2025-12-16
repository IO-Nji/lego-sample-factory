package io.life.order.service;

import io.life.order.entity.OrderAudit;
import io.life.order.repository.OrderAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Import WebhookService if missing
import io.life.order.service.WebhookService;

import java.util.List;

@Service
public class OrderAuditService {
    private final OrderAuditRepository repository;
    private final WebhookService webhookService;

    public OrderAuditService(OrderAuditRepository repository, WebhookService webhookService) {
        this.repository = repository;
        this.webhookService = webhookService;
    }

    @Transactional
    public void createAuditEvent(String orderType, Long orderId, String eventType, String description) {
        OrderAudit audit = new OrderAudit();
        audit.setOrderType(orderType);
        audit.setOrderId(orderId);
        audit.setEventType(eventType);
        audit.setDescription(description);
        OrderAudit saved = repository.save(audit);
        webhookService.dispatch(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderAudit> find(String orderType, Long orderId) {
        return repository.findByOrderTypeAndOrderIdOrderByCreatedAtDesc(orderType, orderId);
    }
}
