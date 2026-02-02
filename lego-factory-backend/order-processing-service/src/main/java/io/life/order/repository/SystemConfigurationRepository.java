package io.life.order.repository;

import io.life.order.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SystemConfiguration entity.
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {

    /**
     * Find configuration by key.
     */
    Optional<SystemConfiguration> findByConfigKey(String configKey);

    /**
     * Check if a configuration key exists.
     */
    boolean existsByConfigKey(String configKey);

    /**
     * Find all configurations in a category.
     */
    List<SystemConfiguration> findByCategory(String category);

    /**
     * Find all editable configurations.
     */
    List<SystemConfiguration> findByEditableTrue();

    /**
     * Find all configurations in a category that are editable.
     */
    List<SystemConfiguration> findByCategoryAndEditableTrue(String category);
}
