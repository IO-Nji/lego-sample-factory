package io.life.masterdata.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.life.masterdata.entity.Product;
import io.life.masterdata.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Product> findById(Long id) {
        return repository.findById(id);
    }

    @SuppressWarnings("null")
    public Product save(Product product) {
        return repository.save(product);
    }

    @SuppressWarnings("null")
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
