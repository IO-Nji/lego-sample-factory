package io.life.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderType; // CUSTOMER, WAREHOUSE, SUPPLY, PRODUCTION

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String eventType; // e.g., STATUS_PROCESSING, COMPLETED, WAREHOUSE_ORDER_CREATED

    @Column(length = 1000)
    private String description;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
