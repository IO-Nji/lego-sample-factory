package io.life.order.controller;

import io.life.order.dto.request.CreateWebhookRequest;
import io.life.order.entity.WebhookSubscription;
import io.life.order.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/webhooks")
@Tag(name = "Webhooks", description = "Webhook subscription management for order events")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "Create webhook subscription", 
               description = "Subscribe to receive HTTP callbacks when specific order events occur")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription created"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping
    public ResponseEntity<WebhookSubscription> create(@RequestBody CreateWebhookRequest req) {
        WebhookSubscription sub = webhookService.subscribe(req.eventType(), req.targetUrl(), req.secret());
        return ResponseEntity.ok(sub);
    }

    @Operation(summary = "List webhook subscriptions", description = "Get all active webhook subscriptions")
    @ApiResponse(responseCode = "200", description = "List of subscriptions")
    @GetMapping
    public ResponseEntity<List<WebhookSubscription>> list() {
        return ResponseEntity.ok(webhookService.list());
    }

    @Operation(summary = "Delete webhook subscription", description = "Unsubscribe from a webhook event")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subscription deleted"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Subscription ID") @PathVariable Long id) {
        webhookService.unsubscribe(id);
        return ResponseEntity.noContent().build();
    }
}
