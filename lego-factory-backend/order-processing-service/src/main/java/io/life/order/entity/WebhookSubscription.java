package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // e.g., CUSTOMER.COMPLETED, CUSTOMER.CANCELLED, ANY

    @Column(nullable = false)
    private String targetUrl;

    @Column
    private String secret; // optional shared secret header

    @Column(nullable = false)
    private Boolean active = true;
}
