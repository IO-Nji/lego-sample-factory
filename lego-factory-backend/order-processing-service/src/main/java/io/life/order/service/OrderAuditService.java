package io.life.order.service;

import io.life.order.entity.OrderAudit;
import io.life.order.repository.OrderAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        
        // Capture user info from request headers
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userIdHeader = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-Authenticated-Role");
                
                if (userIdHeader != null) {
                    try {
                        audit.setUserId(Long.parseLong(userIdHeader));
                    } catch (NumberFormatException e) {
                        // If parsing fails, leave userId as null
                    }
                }
                audit.setUserRole(userRole);
            }
        } catch (Exception e) {
            // If we can't get request attributes, just continue without user info
        }
        
        OrderAudit saved = repository.save(audit);
        webhookService.dispatch(saved);
    }

    // Alias method for backward compatibility
    @Transactional
    public void recordOrderEvent(String orderType, Long orderId, String eventType, String description) {
        this.createAuditEvent(orderType, orderId, eventType, description);
    }

    @Transactional(readOnly = true)
    public List<OrderAudit> find(String orderType, Long orderId) {
        return repository.findByOrderTypeAndOrderIdOrderByCreatedAtDesc(orderType, orderId);
    }
    
    @Transactional(readOnly = true)
    public List<OrderAudit> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }
    
    @Transactional
    public OrderAudit saveAudit(OrderAudit audit) {
        OrderAudit saved = repository.save(audit);
        webhookService.dispatch(saved);
        return saved;
    }
}
