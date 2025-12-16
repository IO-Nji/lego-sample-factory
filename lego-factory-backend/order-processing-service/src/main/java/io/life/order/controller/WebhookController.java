package io.life.order.controller;

import io.life.order.entity.WebhookSubscription;
import io.life.order.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/webhooks")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public static record CreateWebhookRequest(String eventType, String targetUrl, String secret) {}

    @PostMapping
    public ResponseEntity<WebhookSubscription> create(@RequestBody CreateWebhookRequest req) {
        WebhookSubscription sub = webhookService.subscribe(req.eventType(), req.targetUrl(), req.secret());
        return ResponseEntity.ok(sub);
    }

    @GetMapping
    public ResponseEntity<List<WebhookSubscription>> list() {
        return ResponseEntity.ok(webhookService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        webhookService.unsubscribe(id);
        return ResponseEntity.noContent().build();
    }
}
