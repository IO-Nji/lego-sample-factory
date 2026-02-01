package io.life.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String type;

    /**
     * Production workstation ID where this module is manufactured/assembled.
     * 1 = Injection Molding (no parts required - injection molding from raw plastic)
     * 2 = Parts Pre-Production (requires parts)
     * 3 = Part Finishing (requires parts)
     * 4 = Gear Assembly (assembled from parts)
     * 5 = Motor Assembly (assembled from parts)
     */
    @Column(name = "production_workstation_id")
    private Integer productionWorkstationId;
}
