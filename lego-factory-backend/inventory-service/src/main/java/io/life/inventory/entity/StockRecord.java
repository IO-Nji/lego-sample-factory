package io.life.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Version field for optimistic locking.
	 * JPA will automatically check this field during updates.
	 * If another transaction modified the record, OptimisticLockException is thrown.
	 */
	@Version
	private Long version;

	@Column(nullable = false)
	private Long workstationId;

	@Column(nullable = false, length = 50)
	private String itemType;

	@Column(nullable = false)
	private Long itemId;

	@Min(value = 0, message = "Stock quantity cannot be negative")
	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime lastUpdated;

	@PreUpdate
	@PrePersist
	public void updateTimestamp() {
		this.lastUpdated = LocalDateTime.now();
	}
}
