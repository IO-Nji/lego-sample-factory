package io.life.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workstationId; // can represent PW/MS/PSW or workstation

    @Column(nullable = false, length = 50)
    private String itemType; // PRODUCT / MODULE / PART

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Integer delta; // positive credit, negative debit

    @Column(nullable = false)
    private Integer balanceAfter; // resulting on-hand after applying delta

    @Column(nullable = false, length = 64)
    private String reasonCode; // e.g., CUSTOMER_FULFILL, ADJUSTMENT, RECEIPT

    @Column(length = 512)
    private String notes;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
