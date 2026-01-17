package io.life.simal_integration_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String taskId;
    
    private String itemId; // Changed from Long to String to match DTO
    private String itemName;
    private Integer quantity;
    
    private String workstationId;
    private String workstationName;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration; // in minutes
    
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED
    private Integer sequence;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_order_id", nullable = false)
    private ScheduledOrder scheduledOrder;
    
    // Manual scheduling fields
    @Column(nullable = false)
    @Builder.Default
    private Boolean manuallyAdjusted = false;
    
    private String adjustedBy; // username
    private LocalDateTime adjustedAt;
    
    @Column(length = 500)
    private String adjustmentReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (manuallyAdjusted == null) {
            manuallyAdjusted = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
