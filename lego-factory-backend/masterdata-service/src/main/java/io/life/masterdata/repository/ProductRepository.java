package io.life.masterdata.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.life.masterdata.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
