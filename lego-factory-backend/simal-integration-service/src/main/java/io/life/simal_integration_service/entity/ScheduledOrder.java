package io.life.simal_integration_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheduled_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String scheduleId;
    
    private String orderNumber;
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, FAILED
    private LocalDateTime estimatedCompletionTime;
    private Integer totalDuration; // in minutes
    
    @OneToMany(mappedBy = "scheduledOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ScheduledTask> scheduledTasks = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
