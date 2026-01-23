package io.life.inventory.config;

import io.life.inventory.entity.StockRecord;
import io.life.inventory.repository.StockRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * DataInitializer for Inventory Service.
 * Seeds initial stock records for workstations with all product variants.
 * Data is persisted in the H2 file-based database.
 * To reset inventory data, delete the ./data/inventory_db.mv.db file.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

	private final StockRecordRepository stockRecordRepository;

	/**
	 * Seed initial stock records ONLY on first application startup.
	 * Once data is seeded, it is persisted in the file-based H2 database and will
	 * NOT be re-initialized on subsequent application restarts.
	 * 
	 * Creates inventory for:
	 * - Plant Warehouse (ID 7): All product variants
	 * - Modules Supermarket (ID 8): Base modules inventory
	 * - Parts Supply Warehouse (ID 9): All parts inventory
	 * 
	 * To reset data: Delete ./data/inventory_db.mv.db file and restart application.
	 */
	@Override
	public void run(String... args) throws Exception {
		long totalRecords = stockRecordRepository.count();
		
		if (totalRecords == 0) {
			log.info("No stock records found. Seeding initial inventory data...");

			// Plant Warehouse (WS-7) - Stock for all product variants
			// Product variants from MasterdataService:
			// 1: Technic Truck Yellow, 2: Technic Truck Red, 3: Creator House, 4: Friends Cafe, 5-7: Others
			seedPlantWarehouseStock();

			// Modules Supermarket (WS-8) - Base module inventory
			seedModulesSupermarketStock();

			// Parts Supply Warehouse (WS-9) - All parts inventory
			seedPartsSupplyWarehouseStock();

			log.info("✓ Stock records seeded successfully. Data will persist across application restarts.");
		} else {
			log.info("✓ Stock records exist ({} records). Skipping initialization to preserve existing data.", totalRecords);
		}
	}

	/**
	 * Seed Plant Warehouse (WS-7) with initial stock of product variants.
	 * Each product variant gets a default quantity for testing.
	 * 
	 * Products (from masterdata-service):
	 * ID 1: Technic Truck Yellow
	 * ID 2: Technic Truck Red
	 * ID 3: Creator House
	 * ID 4: Friends Cafe
	 */
	private void seedPlantWarehouseStock() {
		Long plantWarehouseId = 7L;

		// Product variant stock levels (itemType = "PRODUCT")
		// Seed with initial quantities to support Scenario 1 (direct fulfillment) testing
		createStockRecord(plantWarehouseId, "PRODUCT", 1L, 15);  // Technic Truck Yellow - 15 units
		createStockRecord(plantWarehouseId, "PRODUCT", 2L, 12);  // Technic Truck Red - 12 units
		createStockRecord(plantWarehouseId, "PRODUCT", 3L, 8);   // Creator House - 8 units
		createStockRecord(plantWarehouseId, "PRODUCT", 4L, 6);   // Friends Cafe - 6 units
		
		log.info("Seeded Plant Warehouse (WS-7) with product variants: Truck Yellow (15), Truck Red (12), House (8), Cafe (6)");
	}

	/**
	 * Seed Modules Supermarket (WS-8) with initial module inventory.
	 * Modules are components used in manufacturing.
	 */
	private void seedModulesSupermarketStock() {
		Long modulesSupermarketId = 8L;

		// Technic Truck modules (Product #1, #2) - IDs 1-6
		createStockRecord(modulesSupermarketId, "MODULE", 1L, 50);  // Module #1 (Truck Chassis)
		createStockRecord(modulesSupermarketId, "MODULE", 2L, 40);  // Module #2 (Truck Drive System)
		createStockRecord(modulesSupermarketId, "MODULE", 3L, 45);  // Module #3 (Truck Wheel Assembly)
		createStockRecord(modulesSupermarketId, "MODULE", 4L, 30);  // Module #4 (Truck Steering Unit)
		createStockRecord(modulesSupermarketId, "MODULE", 5L, 35);  // Module #5 (Truck Light System)
		createStockRecord(modulesSupermarketId, "MODULE", 6L, 25);  // Module #6 (Truck Cab Unit)

		// Creator House modules (Product #3) - IDs 7-11
		createStockRecord(modulesSupermarketId, "MODULE", 7L, 40);  // Module #7 (House Frame)
		createStockRecord(modulesSupermarketId, "MODULE", 8L, 35);  // Module #8 (House Roof)
		createStockRecord(modulesSupermarketId, "MODULE", 9L, 45);  // Module #9 (House Door Assembly)
		createStockRecord(modulesSupermarketId, "MODULE", 10L, 50); // Module #10 (House Window Assembly)
		createStockRecord(modulesSupermarketId, "MODULE", 11L, 60); // Module #11 (House Floor Base)

		// Castle Set modules (Product #4) - IDs 12-16
		createStockRecord(modulesSupermarketId, "MODULE", 12L, 30); // Module #12 (Castle Tower Section)
		createStockRecord(modulesSupermarketId, "MODULE", 13L, 35); // Module #13 (Castle Wall Section)
		createStockRecord(modulesSupermarketId, "MODULE", 14L, 25); // Module #14 (Castle Gate Assembly)
		createStockRecord(modulesSupermarketId, "MODULE", 15L, 40); // Module #15 (Castle Flag Post)
		createStockRecord(modulesSupermarketId, "MODULE", 16L, 28); // Module #16 (Castle Throne Room)

		log.info("Seeded Modules Supermarket (WS-8) with module inventory");
	}

	/**
	 * Seed Parts Supply Warehouse (WS-9) with comprehensive parts inventory.
	 * Parts are the atomic components used to build modules.
	 * High quantities ensure sufficient stock for Scenario 2 testing.
	 */
	private void seedPartsSupplyWarehouseStock() {
		Long partsSupplyWarehouseId = 9L;

		// Structural parts (building blocks) - IDs 1-10
		createStockRecord(partsSupplyWarehouseId, "PART", 1L, 500);   // Brick 2x4 Red
		createStockRecord(partsSupplyWarehouseId, "PART", 2L, 500);   // Brick 2x4 Yellow
		createStockRecord(partsSupplyWarehouseId, "PART", 3L, 500);   // Brick 2x4 Gray
		createStockRecord(partsSupplyWarehouseId, "PART", 4L, 400);   // Plate 2x4 Flat
		createStockRecord(partsSupplyWarehouseId, "PART", 5L, 300);   // Beam 1x8 Technic
		createStockRecord(partsSupplyWarehouseId, "PART", 6L, 300);   // Beam 1x6 Technic
		createStockRecord(partsSupplyWarehouseId, "PART", 7L, 250);   // Slope Brick 45° 2x2
		createStockRecord(partsSupplyWarehouseId, "PART", 8L, 200);   // Panel 1x4x3
		createStockRecord(partsSupplyWarehouseId, "PART", 9L, 150);   // Transparent Window 1x4x3
		createStockRecord(partsSupplyWarehouseId, "PART", 10L, 100);  // Door Frame 1x4x6

		// Mechanical parts (moving/functional) - IDs 11-20
		createStockRecord(partsSupplyWarehouseId, "PART", 11L, 200);  // Wheel Rim 56x34
		createStockRecord(partsSupplyWarehouseId, "PART", 12L, 200);  // Tire 68.82x34.4
		createStockRecord(partsSupplyWarehouseId, "PART", 13L, 300);  // Axle 6L
		createStockRecord(partsSupplyWarehouseId, "PART", 14L, 250);  // Axle 8L
		createStockRecord(partsSupplyWarehouseId, "PART", 15L, 200);  // Gear 16 Tooth
		createStockRecord(partsSupplyWarehouseId, "PART", 16L, 200);  // Gear 24 Tooth
		createStockRecord(partsSupplyWarehouseId, "PART", 17L, 600);  // Connector Pin (high qty)
		createStockRecord(partsSupplyWarehouseId, "PART", 18L, 500);  // Bushing Half (high qty)
		createStockRecord(partsSupplyWarehouseId, "PART", 19L, 50);   // Turntable Large
		createStockRecord(partsSupplyWarehouseId, "PART", 20L, 150);  // Hinge Plate 1x2

		// Electrical parts (powered components) - IDs 21-28
		createStockRecord(partsSupplyWarehouseId, "PART", 21L, 100);  // Motor XL
		createStockRecord(partsSupplyWarehouseId, "PART", 22L, 150);  // Motor M
		createStockRecord(partsSupplyWarehouseId, "PART", 23L, 200);  // LED White
		createStockRecord(partsSupplyWarehouseId, "PART", 24L, 200);  // LED Red
		createStockRecord(partsSupplyWarehouseId, "PART", 25L, 200);  // LED Yellow
		createStockRecord(partsSupplyWarehouseId, "PART", 26L, 100);  // Battery Box AAA
		createStockRecord(partsSupplyWarehouseId, "PART", 27L, 300);  // Wire 20cm
		createStockRecord(partsSupplyWarehouseId, "PART", 28L, 150);  // Switch On/Off

		// Decorative parts (finishing touches) - IDs 29-36
		createStockRecord(partsSupplyWarehouseId, "PART", 29L, 100);  // Minifig Knight
		createStockRecord(partsSupplyWarehouseId, "PART", 30L, 100);  // Minifig Driver
		createStockRecord(partsSupplyWarehouseId, "PART", 31L, 80);   // Flag Triangular Red
		createStockRecord(partsSupplyWarehouseId, "PART", 32L, 120);  // Windshield 4x4x2
		createStockRecord(partsSupplyWarehouseId, "PART", 33L, 150);  // Plant Leaves Green
		createStockRecord(partsSupplyWarehouseId, "PART", 34L, 300);  // Roof Tile 2x2
		createStockRecord(partsSupplyWarehouseId, "PART", 35L, 200);  // Fence 1x4x2
		createStockRecord(partsSupplyWarehouseId, "PART", 36L, 50);   // Treasure Chest

		log.info("Seeded Parts Supply Warehouse (WS-9) with comprehensive parts inventory (36 part types)");
	}

	/**
	 * Helper method to create and save a stock record.
	 */
	private void createStockRecord(Long workstationId, String itemType, Long itemId, Integer quantity) {
		StockRecord record = new StockRecord();
		record.setWorkstationId(workstationId);
		record.setItemType(itemType);
		record.setItemId(itemId);
		record.setQuantity(quantity);
		record.setLastUpdated(LocalDateTime.now());

		stockRecordRepository.save(record);
		log.debug("Created stock record: WS-{} | {} #{} | Qty: {}",
				workstationId, itemType, itemId, quantity);
	}
}
