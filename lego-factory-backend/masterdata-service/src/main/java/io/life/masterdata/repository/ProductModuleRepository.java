package io.life.masterdata.repository;

import io.life.masterdata.entity.ProductModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductModuleRepository extends JpaRepository<ProductModule, Long> {
    List<ProductModule> findByProductId(Long productId);
    List<ProductModule> findByModuleId(Long moduleId);
}
