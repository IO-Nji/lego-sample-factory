package io.life.inventory.service;

import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.StockRecordRepository;
import io.life.inventory.dto.StockRecordDto;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockRecordService {

	private final StockRecordRepository repository;
	private final MasterdataClient masterdataClient;

	public List<StockRecordDto> findAll() {
		return repository.findAll().stream()
				.map(this::toDtoEnriched)
				.collect(Collectors.toList());
	}

	@SuppressWarnings("null")
	public StockRecordDto findById(Long id) {
		return repository.findById(id)
				.map(this::toDtoEnriched)
				.orElse(null);
	}

	public List<StockRecordDto> getStockByWorkstationId(Long workstationId) {
		return repository.findByWorkstationId(workstationId).stream()
				.map(this::toDtoEnriched)
				.collect(Collectors.toList());
	}

	public StockRecordDto getStockByWorkstationAndItem(Long workstationId, String itemType, Long itemId) {
		return repository.findByWorkstationIdAndItemTypeAndItemId(workstationId, itemType, itemId)
				.map(this::toDtoEnriched)
				.orElse(null);
	}

	/**
	 * Update stock quantity with optimistic locking and automatic retry on conflicts.
	 * 
	 * Uses @Version field in StockRecord entity to detect concurrent modifications.
	 * If another transaction modified the record, JPA throws OptimisticLockException.
	 * Spring Retry will automatically retry up to 3 times with exponential backoff.
	 * 
	 * @param workstationId the workstation ID
	 * @param itemType the item type (PRODUCT, MODULE, PART)
	 * @param itemId the item ID
	 * @param quantity the new quantity (must be >= 0)
	 * @return the updated stock record DTO
	 * @throws IllegalArgumentException if quantity is negative
	 * @throws ObjectOptimisticLockingFailureException if retry attempts exhausted
	 */
	@Retryable(
		retryFor = { OptimisticLockException.class, ObjectOptimisticLockingFailureException.class },
		maxAttempts = 3,
		backoff = @Backoff(delay = 100, multiplier = 2)
	)
	@Transactional
	public StockRecordDto updateStock(Long workstationId, String itemType, Long itemId, Integer quantity) {
		// Validate non-negative quantity
		if (quantity < 0) {
			throw new IllegalArgumentException(
				String.format("Stock quantity cannot be negative: workstationId=%d, itemType=%s, itemId=%d, quantity=%d",
					workstationId, itemType, itemId, quantity));
		}

		Optional<StockRecord> existing = repository.findByWorkstationIdAndItemTypeAndItemId(
				workstationId, itemType, itemId);

		StockRecord stockRecord;
		if (existing.isPresent()) {
			stockRecord = existing.get();
			log.debug("Updating existing stock record: id={}, version={}, oldQty={}, newQty={}",
				stockRecord.getId(), stockRecord.getVersion(), stockRecord.getQuantity(), quantity);
			stockRecord.setQuantity(quantity);
			stockRecord.setLastUpdated(LocalDateTime.now());
		} else {
			stockRecord = new StockRecord();
			stockRecord.setWorkstationId(workstationId);
			stockRecord.setItemType(itemType);
			stockRecord.setItemId(itemId);
			stockRecord.setQuantity(quantity);
			stockRecord.setLastUpdated(LocalDateTime.now());
			log.debug("Creating new stock record: workstationId={}, itemType={}, itemId={}, qty={}",
				workstationId, itemType, itemId, quantity);
		}

		try {
			StockRecord saved = repository.save(stockRecord);
			log.debug("Stock record saved successfully: id={}, version={}", saved.getId(), saved.getVersion());
			return toDto(saved);
		} catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
			log.warn("Optimistic lock conflict detected for stock update - will retry. " +
				"workstationId={}, itemType={}, itemId={}", workstationId, itemType, itemId);
			throw e; // @Retryable will catch and retry
		}
	}

	/**
	 * Adjust stock by delta amount with optimistic locking protection.
	 * This is the preferred method for concurrent stock adjustments.
	 * 
	 * @param workstationId the workstation ID
	 * @param itemType the item type (PRODUCT, MODULE, PART)
	 * @param itemId the item ID
	 * @param delta the amount to add (positive) or subtract (negative)
	 * @return the updated stock record DTO
	 * @throws IllegalArgumentException if resulting quantity would be negative
	 * @throws ObjectOptimisticLockingFailureException if retry attempts exhausted
	 */
	@Retryable(
		retryFor = { OptimisticLockException.class, ObjectOptimisticLockingFailureException.class },
		maxAttempts = 3,
		backoff = @Backoff(delay = 100, multiplier = 2)
	)
	@Transactional
	public StockRecordDto adjustStock(Long workstationId, String itemType, Long itemId, Integer delta) {
		Optional<StockRecord> existing = repository.findByWorkstationIdAndItemTypeAndItemId(
				workstationId, itemType, itemId);

		StockRecord stockRecord;
		int currentQty = 0;
		
		if (existing.isPresent()) {
			stockRecord = existing.get();
			currentQty = stockRecord.getQuantity();
		} else {
			stockRecord = new StockRecord();
			stockRecord.setWorkstationId(workstationId);
			stockRecord.setItemType(itemType);
			stockRecord.setItemId(itemId);
		}

		int newQty = currentQty + delta;
		
		// Validate non-negative result
		if (newQty < 0) {
			throw new IllegalArgumentException(
				String.format("Stock adjustment would result in negative quantity: " +
					"workstationId=%d, itemType=%s, itemId=%d, currentQty=%d, delta=%d, resultQty=%d",
					workstationId, itemType, itemId, currentQty, delta, newQty));
		}

		log.debug("Adjusting stock: workstationId={}, itemType={}, itemId={}, currentQty={}, delta={}, newQty={}",
			workstationId, itemType, itemId, currentQty, delta, newQty);
		
		stockRecord.setQuantity(newQty);
		stockRecord.setLastUpdated(LocalDateTime.now());

		try {
			StockRecord saved = repository.save(stockRecord);
			log.debug("Stock adjustment saved: id={}, version={}, finalQty={}", 
				saved.getId(), saved.getVersion(), saved.getQuantity());
			return toDto(saved);
		} catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
			log.warn("Optimistic lock conflict during stock adjustment - will retry. " +
				"workstationId={}, itemType={}, itemId={}, delta={}", 
				workstationId, itemType, itemId, delta);
			throw e; // @Retryable will catch and retry
		}
	}

	@Transactional
	public StockRecordDto save(StockRecordDto dto) {
		// Validate non-negative quantity
		if (dto.getQuantity() < 0) {
			throw new IllegalArgumentException("Stock quantity cannot be negative: " + dto.getQuantity());
		}

		StockRecord stockRecord = new StockRecord();
		stockRecord.setWorkstationId(dto.getWorkstationId());
		stockRecord.setItemType(dto.getItemType());
		stockRecord.setItemId(dto.getItemId());
		stockRecord.setQuantity(dto.getQuantity());
		stockRecord.setLastUpdated(LocalDateTime.now());

		StockRecord saved = repository.save(stockRecord);
		return toDto(saved);
	}

	@SuppressWarnings("null")
	@Transactional
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	private StockRecordDto toDto(StockRecord entity) {
		return new StockRecordDto(
				entity.getId(),
				entity.getWorkstationId(),
				entity.getItemType(),
				entity.getItemId(),
				null, // itemName - to be enriched by caller if needed
				entity.getQuantity(),
				entity.getLastUpdated()
		);
	}

	/**
	 * Convert StockRecord to DTO with enriched item name from masterdata-service.
	 */
	private StockRecordDto toDtoEnriched(StockRecord entity) {
		String itemName = masterdataClient.getItemName(entity.getItemType(), entity.getItemId());
		return new StockRecordDto(
				entity.getId(),
				entity.getWorkstationId(),
				entity.getItemType(),
				entity.getItemId(),
				itemName,
				entity.getQuantity(),
				entity.getLastUpdated()
		);
	}

}
