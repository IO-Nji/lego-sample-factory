package io.life.order.service;

import io.life.order.entity.OrderAudit;
import io.life.order.entity.WebhookSubscription;
import io.life.order.repository.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private final WebhookSubscriptionRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookService(WebhookSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebhookSubscription subscribe(String eventType, String targetUrl, String secret) {
        WebhookSubscription sub = new WebhookSubscription();
        sub.setEventType(eventType == null || eventType.isBlank() ? "ANY" : eventType);
        sub.setTargetUrl(targetUrl);
        sub.setSecret(secret);
        sub.setActive(true);
        return repository.save(sub);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> list() {
        return repository.findByActiveTrue();
    }

    @Transactional
    public void unsubscribe(Long id) {
        repository.deleteById(id);
    }

    public void dispatch(OrderAudit audit) {
        List<WebhookSubscription> subs = repository.findByActiveTrue();
        if (subs.isEmpty()) return;

        String eventKey = audit.getOrderType() + "." + audit.getEventType();
        for (WebhookSubscription sub : subs) {
            if (!"ANY".equalsIgnoreCase(sub.getEventType()) && !sub.getEventType().equalsIgnoreCase(eventKey)) {
                continue;
            }
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (sub.getSecret() != null && !sub.getSecret().isBlank()) {
                    headers.add("X-Webhook-Secret", sub.getSecret());
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("orderType", audit.getOrderType());
                payload.put("orderId", audit.getOrderId());
                payload.put("eventType", audit.getEventType());
                payload.put("description", audit.getDescription());
                payload.put("createdAt", audit.getCreatedAt());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(sub.getTargetUrl(), entity, String.class);
                logger.info("Webhook delivered to {} for {}", sub.getTargetUrl(), eventKey);
            } catch (Exception e) {
                logger.warn("Webhook delivery failed to {}: {}", sub.getTargetUrl(), e.getMessage());
            }
        }
    }
}
