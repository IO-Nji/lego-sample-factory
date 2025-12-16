package io.life.inventory.repository;

import io.life.inventory.entity.StockLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, Long> {
    List<StockLedgerEntry> findTop100ByOrderByCreatedAtDesc();
    List<StockLedgerEntry> findByWorkstationIdOrderByCreatedAtDesc(Long workstationId);
    List<StockLedgerEntry> findByItemTypeAndItemIdOrderByCreatedAtDesc(String itemType, Long itemId);
    List<StockLedgerEntry> findByWorkstationIdAndItemTypeAndItemIdOrderByCreatedAtDesc(Long workstationId, String itemType, Long itemId);
}
