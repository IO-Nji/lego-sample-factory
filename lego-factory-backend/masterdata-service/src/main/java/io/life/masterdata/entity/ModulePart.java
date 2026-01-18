package io.life.masterdata.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "module_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModulePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long moduleId;

    @Column(nullable = false)
    private Long partId;

    @Column(nullable = false)
    private Integer quantity;

    public ModulePart(Long moduleId, Long partId, Integer quantity) {
        this.moduleId = moduleId;
        this.partId = partId;
        this.quantity = quantity;
    }
}
