package io.life.masterdata.service;

import io.life.masterdata.entity.ProductModule;
import io.life.masterdata.repository.ProductModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductModuleService {
    private final ProductModuleRepository repository;

    public ProductModule save(ProductModule productModule) {
        return repository.save(productModule);
    }

    public List<ProductModule> findAll() {
        return repository.findAll();
    }

    public List<ProductModule> findByProductVariantId(Long productVariantId) {
        return repository.findByProductVariantId(productVariantId);
    }

    public List<ProductModule> findByModuleId(Long moduleId) {
        return repository.findByModuleId(moduleId);
    }
}
