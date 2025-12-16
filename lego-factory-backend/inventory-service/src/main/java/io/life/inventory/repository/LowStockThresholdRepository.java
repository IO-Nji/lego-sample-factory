package io.life.inventory.repository;

import io.life.inventory.entity.LowStockThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LowStockThresholdRepository extends JpaRepository<LowStockThreshold, Long> {
    List<LowStockThreshold> findByWorkstationId(Long workstationId);
    List<LowStockThreshold> findByWorkstationIdIsNull();
    Optional<LowStockThreshold> findByWorkstationIdAndItemTypeAndItemId(Long workstationId, String itemType, Long itemId);
    Optional<LowStockThreshold> findByItemTypeAndItemIdAndWorkstationIdIsNull(String itemType, Long itemId);
}
