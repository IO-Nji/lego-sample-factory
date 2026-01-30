package io.life.order.dto.request;

/**
 * Request DTO for creating a webhook subscription.
 * 
 * @param eventType The type of event to subscribe to (e.g., "ORDER_COMPLETED", "ORDER_STARTED")
 * @param targetUrl The URL to send webhook notifications to
 * @param secret Optional secret for webhook signature verification
 */
public record CreateWebhookRequest(
    String eventType,
    String targetUrl,
    String secret
) {}
