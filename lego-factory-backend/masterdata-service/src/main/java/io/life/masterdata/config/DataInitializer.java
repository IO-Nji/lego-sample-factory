package io.life.masterdata.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.life.masterdata.entity.Module;
import io.life.masterdata.entity.ModulePart;
import io.life.masterdata.entity.Part;
import io.life.masterdata.entity.ProductModule;
import io.life.masterdata.entity.Product;
import io.life.masterdata.service.ModulePartService;
import io.life.masterdata.service.ModuleService;
import io.life.masterdata.service.PartService;
import io.life.masterdata.service.ProductModuleService;
import io.life.masterdata.service.ProductService;
import io.life.masterdata.service.WorkstationService;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String TYPE_MANUFACTURING = "MANUFACTURING";
    private static final String TYPE_ASSEMBLY = "ASSEMBLY";
    private static final String TYPE_WAREHOUSE = "WAREHOUSE";
    
    // Module and Part type constants
    private static final String TYPE_MECHANICAL = "MECHANICAL";
    private static final String TYPE_STRUCTURAL = "STRUCTURAL";
    private static final String TYPE_ELECTRICAL = "ELECTRICAL";
    private static final String TYPE_DECORATIVE = "DECORATIVE";

    private final ProductService productService;
    private final ModuleService moduleService;
    private final PartService partService;
    private final WorkstationService workstationService;
    private final ProductModuleService productModuleService;
    private final ModulePartService modulePartService;

    public DataInitializer(ProductService productService, ModuleService moduleService,
            PartService partService, WorkstationService workstationService,
            ProductModuleService productModuleService, ModulePartService modulePartService) {
        this.productService = productService;
        this.moduleService = moduleService;
        this.partService = partService;
        this.workstationService = workstationService;
        this.productModuleService = productModuleService;
        this.modulePartService = modulePartService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed workstations
        if (workstationService.findAll().isEmpty()) {
            workstationService.save(createWorkstationDto(null, "Injection Molding Station", TYPE_MANUFACTURING, "Plastic injection molding equipment", true));
            workstationService.save(createWorkstationDto(null, "Parts Pre-Production", TYPE_MANUFACTURING, "Parts preparation and assembly", true));
            workstationService.save(createWorkstationDto(null, "Part Finishing", TYPE_MANUFACTURING, "Part finishing and quality control", true));
            workstationService.save(createWorkstationDto(null, "Gear Assembly", TYPE_ASSEMBLY, "Gear assembly station", true));
            workstationService.save(createWorkstationDto(null, "Motor Assembly", TYPE_ASSEMBLY, "Motor assembly station", true));
            workstationService.save(createWorkstationDto(null, "Final Assembly", TYPE_ASSEMBLY, "Final assembly station", true));
            workstationService.save(createWorkstationDto(null, "Plant Warehouse", TYPE_WAREHOUSE, "Main warehouse for Plant", true));
            workstationService.save(createWorkstationDto(null, "Modules Supermarket", TYPE_WAREHOUSE, "Modules inventory point", true));
            workstationService.save(createWorkstationDto(null, "Parts Supply Warehouse", TYPE_WAREHOUSE, "Parts supply warehouse", true));
        }

        // Seed products (Final Products)
        if (productService.findAll().isEmpty()) {
            productService.save(new Product(null, "Technic Truck Yellow", "Yellow motorized LEGO Technic truck with working steering", 149.99, 240));
            productService.save(new Product(null, "Technic Truck Red", "Red motorized LEGO Technic truck with working steering", 149.99, 240));
            productService.save(new Product(null, "Creator House", "Modular 3-story house with detailed interior", 99.99, 180));
            productService.save(new Product(null, "Castle Set", "Medieval castle with towers and drawbridge", 129.99, 200));
        }

        // Seed modules (Sub-assemblies made from parts)
        if (moduleService.findAll().isEmpty()) {
            // Truck modules (used by both Technic Trucks)
            // WS-4 = Gear Assembly, WS-5 = Motor Assembly, WS-2 = Parts Pre-Production, WS-1 = Injection Molding
            moduleService.save(new Module(null, "Truck Chassis", "Heavy-duty frame with axle mounts", TYPE_MECHANICAL, 2)); // WS-2
            moduleService.save(new Module(null, "Truck Drive System", "Motorized drivetrain with gearbox", TYPE_MECHANICAL, 5)); // WS-5 Motor
            moduleService.save(new Module(null, "Truck Wheel Assembly", "Complete wheel set with tires and axles", TYPE_MECHANICAL, 2)); // WS-2
            moduleService.save(new Module(null, "Truck Steering Unit", "Front wheel steering mechanism", TYPE_MECHANICAL, 4)); // WS-4 Gear
            moduleService.save(new Module(null, "Truck Light System", "LED headlights and taillights", TYPE_ELECTRICAL, 2)); // WS-2
            moduleService.save(new Module(null, "Truck Cab Unit", "Driver cabin with windshield", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            
            // House modules (used by Creator House)
            moduleService.save(new Module(null, "House Wall Panel", "Pre-assembled wall section with studs", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            moduleService.save(new Module(null, "House Roof Assembly", "Pitched roof with tiles", TYPE_STRUCTURAL, 3)); // WS-3 Finishing
            moduleService.save(new Module(null, "House Window Frame", "Window with opening shutters", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            moduleService.save(new Module(null, "House Door Unit", "Entry door with hinges", TYPE_STRUCTURAL, 2)); // WS-2
            moduleService.save(new Module(null, "House Floor Base", "Foundation platform with connectors", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            
            // Castle modules (used by Castle Set)
            moduleService.save(new Module(null, "Castle Tower Section", "Round tower segment with battlements", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            moduleService.save(new Module(null, "Castle Wall Section", "Fortified wall segment", TYPE_STRUCTURAL, 1)); // WS-1 Injection
            moduleService.save(new Module(null, "Castle Gate Assembly", "Working drawbridge mechanism", TYPE_MECHANICAL, 4)); // WS-4 Gear
            moduleService.save(new Module(null, "Castle Flag Post", "Tower flag with pole", TYPE_DECORATIVE, 3)); // WS-3 Finishing
            moduleService.save(new Module(null, "Castle Throne Room", "Interior decoration set", TYPE_DECORATIVE, 2)); // WS-2
        }

        // Seed parts (Basic components - raw materials)
        if (partService.findAll().isEmpty()) {
            // Structural parts (injection molded basics)
            partService.save(new Part(null, "Brick 2x4 Red", "Standard red building brick", TYPE_STRUCTURAL, 0.25));
            partService.save(new Part(null, "Brick 2x4 Yellow", "Standard yellow building brick", TYPE_STRUCTURAL, 0.25));
            partService.save(new Part(null, "Brick 2x4 Gray", "Standard gray building brick", TYPE_STRUCTURAL, 0.25));
            partService.save(new Part(null, "Plate 2x4 Flat", "Thin flat plate connector", TYPE_STRUCTURAL, 0.15));
            partService.save(new Part(null, "Beam 1x8 Technic", "Long structural beam with holes", TYPE_STRUCTURAL, 0.35));
            partService.save(new Part(null, "Beam 1x6 Technic", "Medium structural beam", TYPE_STRUCTURAL, 0.30));
            partService.save(new Part(null, "Slope Brick 45° 2x2", "Angled roof/body panel", TYPE_STRUCTURAL, 0.30));
            partService.save(new Part(null, "Panel 1x4x3", "Solid wall panel piece", TYPE_STRUCTURAL, 0.40));
            partService.save(new Part(null, "Transparent Window 1x4x3", "Clear window piece", TYPE_STRUCTURAL, 0.50));
            partService.save(new Part(null, "Door Frame 1x4x6", "Standard door frame", TYPE_STRUCTURAL, 0.60));
            
            // Mechanical parts (moving/functional components)
            partService.save(new Part(null, "Wheel Rim 56x34", "Truck wheel rim", TYPE_MECHANICAL, 1.20));
            partService.save(new Part(null, "Tire 68.82x34.4", "Large rubber tire", TYPE_MECHANICAL, 1.50));
            partService.save(new Part(null, "Axle 6L", "6-stud length axle rod", TYPE_MECHANICAL, 0.20));
            partService.save(new Part(null, "Axle 8L", "8-stud length axle rod", TYPE_MECHANICAL, 0.25));
            partService.save(new Part(null, "Gear 16 Tooth", "Medium drive gear", TYPE_MECHANICAL, 0.45));
            partService.save(new Part(null, "Gear 24 Tooth", "Large drive gear", TYPE_MECHANICAL, 0.55));
            partService.save(new Part(null, "Connector Pin", "Standard friction pin", TYPE_MECHANICAL, 0.10));
            partService.save(new Part(null, "Bushing Half", "Half-width spacer", TYPE_MECHANICAL, 0.08));
            partService.save(new Part(null, "Turntable Large", "Rotating base plate", TYPE_MECHANICAL, 2.50));
            partService.save(new Part(null, "Hinge Plate 1x2", "Folding hinge connector", TYPE_MECHANICAL, 0.35));
            
            // Electrical parts (powered components)
            partService.save(new Part(null, "Motor XL", "Large power motor", TYPE_ELECTRICAL, 12.99));
            partService.save(new Part(null, "Motor M", "Medium power motor", TYPE_ELECTRICAL, 8.99));
            partService.save(new Part(null, "LED White", "White LED light element", TYPE_ELECTRICAL, 2.50));
            partService.save(new Part(null, "LED Red", "Red LED light element", TYPE_ELECTRICAL, 2.50));
            partService.save(new Part(null, "LED Yellow", "Yellow LED light element", TYPE_ELECTRICAL, 2.50));
            partService.save(new Part(null, "Battery Box AAA", "Power supply unit", TYPE_ELECTRICAL, 5.99));
            partService.save(new Part(null, "Wire 20cm", "Electrical connector cable", TYPE_ELECTRICAL, 0.75));
            partService.save(new Part(null, "Switch On/Off", "Power control switch", TYPE_ELECTRICAL, 1.50));
            
            // Decorative parts (finishing touches)
            partService.save(new Part(null, "Minifig Knight", "Castle knight figure", TYPE_DECORATIVE, 3.99));
            partService.save(new Part(null, "Minifig Driver", "Truck driver figure", TYPE_DECORATIVE, 3.99));
            partService.save(new Part(null, "Flag Triangular Red", "Castle banner flag", TYPE_DECORATIVE, 0.85));
            partService.save(new Part(null, "Windshield 4x4x2", "Curved truck windshield", TYPE_DECORATIVE, 1.25));
            partService.save(new Part(null, "Plant Leaves Green", "Decorative foliage", TYPE_DECORATIVE, 0.40));
            partService.save(new Part(null, "Roof Tile 2x2", "Textured roof piece", TYPE_DECORATIVE, 0.30));
            partService.save(new Part(null, "Fence 1x4x2", "Railing segment", TYPE_DECORATIVE, 0.45));
            partService.save(new Part(null, "Treasure Chest", "Castle accessory", TYPE_DECORATIVE, 1.50));
        }

        // Seed Product-Module relationships (Bill of Materials)
        if (productModuleService.findAll().isEmpty()) {
            // Technic Truck Yellow (ID=1) - uses truck modules
            productModuleService.save(new ProductModule(1L, 1L, 1)); // Truck Chassis x1
            productModuleService.save(new ProductModule(1L, 2L, 1)); // Truck Drive System x1
            productModuleService.save(new ProductModule(1L, 3L, 1)); // Truck Wheel Assembly x1
            productModuleService.save(new ProductModule(1L, 4L, 1)); // Truck Steering Unit x1
            productModuleService.save(new ProductModule(1L, 5L, 1)); // Truck Light System x1
            productModuleService.save(new ProductModule(1L, 6L, 1)); // Truck Cab Unit x1

            // Technic Truck Red (ID=2) - uses same truck modules
            productModuleService.save(new ProductModule(2L, 1L, 1)); // Truck Chassis x1
            productModuleService.save(new ProductModule(2L, 2L, 1)); // Truck Drive System x1
            productModuleService.save(new ProductModule(2L, 3L, 1)); // Truck Wheel Assembly x1
            productModuleService.save(new ProductModule(2L, 4L, 1)); // Truck Steering Unit x1
            productModuleService.save(new ProductModule(2L, 5L, 1)); // Truck Light System x1
            productModuleService.save(new ProductModule(2L, 6L, 1)); // Truck Cab Unit x1

            // Creator House (ID=3) - uses house modules
            productModuleService.save(new ProductModule(3L, 7L, 4));  // House Wall Panel x4
            productModuleService.save(new ProductModule(3L, 8L, 1));  // House Roof Assembly x1
            productModuleService.save(new ProductModule(3L, 9L, 3));  // House Window Frame x3
            productModuleService.save(new ProductModule(3L, 10L, 1)); // House Door Unit x1
            productModuleService.save(new ProductModule(3L, 11L, 3)); // House Floor Base x3 (3 floors)

            // Castle Set (ID=4) - uses castle modules
            productModuleService.save(new ProductModule(4L, 12L, 4)); // Castle Tower Section x4
            productModuleService.save(new ProductModule(4L, 13L, 8)); // Castle Wall Section x8
            productModuleService.save(new ProductModule(4L, 14L, 1)); // Castle Gate Assembly x1
            productModuleService.save(new ProductModule(4L, 15L, 4)); // Castle Flag Post x4
            productModuleService.save(new ProductModule(4L, 16L, 1)); // Castle Throne Room x1
        }

        // Seed Module-Part relationships (Module Bill of Materials)
        if (modulePartService.findAll().isEmpty()) {
            // Truck Chassis (Module ID=1)
            modulePartService.save(new ModulePart(1L, 5L, 8));  // Beam 1x8 Technic x8
            modulePartService.save(new ModulePart(1L, 6L, 4));  // Beam 1x6 Technic x4
            modulePartService.save(new ModulePart(1L, 17L, 12)); // Connector Pin x12

            // Truck Drive System (Module ID=2)
            modulePartService.save(new ModulePart(2L, 21L, 1));  // Motor XL x1
            modulePartService.save(new ModulePart(2L, 15L, 2));  // Gear 16 Tooth x2
            modulePartService.save(new ModulePart(2L, 16L, 2));  // Gear 24 Tooth x2
            modulePartService.save(new ModulePart(2L, 13L, 2));  // Axle 6L x2

            // Truck Wheel Assembly (Module ID=3)
            modulePartService.save(new ModulePart(3L, 11L, 4));  // Wheel Rim 56x34 x4
            modulePartService.save(new ModulePart(3L, 12L, 4));  // Tire 68.82x34.4 x4
            modulePartService.save(new ModulePart(3L, 14L, 2));  // Axle 8L x2
            modulePartService.save(new ModulePart(3L, 18L, 8));  // Bushing Half x8

            // Truck Steering Unit (Module ID=4)
            modulePartService.save(new ModulePart(4L, 19L, 1));  // Turntable Large x1
            modulePartService.save(new ModulePart(4L, 13L, 2));  // Axle 6L x2
            modulePartService.save(new ModulePart(4L, 15L, 2));  // Gear 16 Tooth x2
            modulePartService.save(new ModulePart(4L, 17L, 4));  // Connector Pin x4

            // Truck Light System (Module ID=5)
            modulePartService.save(new ModulePart(5L, 23L, 2));  // LED White x2 (headlights)
            modulePartService.save(new ModulePart(5L, 24L, 2));  // LED Red x2 (taillights)
            modulePartService.save(new ModulePart(5L, 26L, 1));  // Battery Box AAA x1
            modulePartService.save(new ModulePart(5L, 27L, 4));  // Wire 20cm x4
            modulePartService.save(new ModulePart(5L, 28L, 1));  // Switch On/Off x1

            // Truck Cab Unit (Module ID=6)
            modulePartService.save(new ModulePart(6L, 1L, 12));  // Brick 2x4 Yellow x12
            modulePartService.save(new ModulePart(6L, 7L, 6));   // Slope Brick 45° 2x2 x6
            modulePartService.save(new ModulePart(6L, 32L, 1));  // Windshield 4x4x2 x1
            modulePartService.save(new ModulePart(6L, 30L, 1));  // Minifig Driver x1

            // House Wall Panel (Module ID=7)
            modulePartService.save(new ModulePart(7L, 1L, 16));  // Brick 2x4 Red x16
            modulePartService.save(new ModulePart(7L, 4L, 8));   // Plate 2x4 Flat x8
            modulePartService.save(new ModulePart(7L, 8L, 6));   // Panel 1x4x3 x6

            // House Roof Assembly (Module ID=8)
            modulePartService.save(new ModulePart(8L, 7L, 24));  // Slope Brick 45° 2x2 x24
            modulePartService.save(new ModulePart(8L, 34L, 32)); // Roof Tile 2x2 x32
            modulePartService.save(new ModulePart(8L, 4L, 12));  // Plate 2x4 Flat x12

            // House Window Frame (Module ID=9)
            modulePartService.save(new ModulePart(9L, 9L, 1));   // Transparent Window 1x4x3 x1
            modulePartService.save(new ModulePart(9L, 1L, 4));   // Brick 2x4 Red x4
            modulePartService.save(new ModulePart(9L, 20L, 2));  // Hinge Plate 1x2 x2

            // House Door Unit (Module ID=10)
            modulePartService.save(new ModulePart(10L, 10L, 1)); // Door Frame 1x4x6 x1
            modulePartService.save(new ModulePart(10L, 20L, 2)); // Hinge Plate 1x2 x2
            modulePartService.save(new ModulePart(10L, 1L, 6));  // Brick 2x4 Red x6

            // House Floor Base (Module ID=11)
            modulePartService.save(new ModulePart(11L, 4L, 24)); // Plate 2x4 Flat x24
            modulePartService.save(new ModulePart(11L, 1L, 8));  // Brick 2x4 Red x8

            // Castle Tower Section (Module ID=12)
            modulePartService.save(new ModulePart(12L, 3L, 20)); // Brick 2x4 Gray x20
            modulePartService.save(new ModulePart(12L, 7L, 8));  // Slope Brick 45° 2x2 x8
            modulePartService.save(new ModulePart(12L, 29L, 1)); // Minifig Knight x1

            // Castle Wall Section (Module ID=13)
            modulePartService.save(new ModulePart(13L, 3L, 16)); // Brick 2x4 Gray x16
            modulePartService.save(new ModulePart(13L, 8L, 6));  // Panel 1x4x3 x6
            modulePartService.save(new ModulePart(13L, 35L, 4)); // Fence 1x4x2 x4 (battlements)

            // Castle Gate Assembly (Module ID=14)
            modulePartService.save(new ModulePart(14L, 3L, 24)); // Brick 2x4 Gray x24
            modulePartService.save(new ModulePart(14L, 20L, 4)); // Hinge Plate 1x2 x4 (drawbridge)
            modulePartService.save(new ModulePart(14L, 10L, 1)); // Door Frame 1x4x6 x1
            modulePartService.save(new ModulePart(14L, 27L, 2)); // Wire 20cm x2 (chains)

            // Castle Flag Post (Module ID=15)
            modulePartService.save(new ModulePart(15L, 13L, 1)); // Axle 6L x1 (pole)
            modulePartService.save(new ModulePart(15L, 31L, 1)); // Flag Triangular Red x1
            modulePartService.save(new ModulePart(15L, 3L, 2));  // Brick 2x4 Gray x2

            // Castle Throne Room (Module ID=16)
            modulePartService.save(new ModulePart(16L, 3L, 12)); // Brick 2x4 Gray x12
            modulePartService.save(new ModulePart(16L, 36L, 1)); // Treasure Chest x1
            modulePartService.save(new ModulePart(16L, 29L, 2)); // Minifig Knight x2
            modulePartService.save(new ModulePart(16L, 33L, 4)); // Plant Leaves Green x4
        }
    }

    private io.life.masterdata.dto.WorkstationDto createWorkstationDto(Long id, String name, String type, String description, Boolean active) {
        io.life.masterdata.dto.WorkstationDto dto = new io.life.masterdata.dto.WorkstationDto();
        dto.setId(id);
        dto.setName(name);
        dto.setWorkstationType(type);
        dto.setDescription(description);
        dto.setActive(active);
        return dto;
    }
}

