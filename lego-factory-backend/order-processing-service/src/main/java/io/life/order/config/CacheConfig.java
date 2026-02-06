package io.life.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for order-processing-service.
 * Provides caching for masterdata (products, modules, parts) to reduce
 * inter-service calls and improve response times.
 * 
 * Cache Names:
 * - productModules: Product BOM (modules) - 1 hour TTL
 * - moduleParts: Module BOM (parts) - 1 hour TTL
 * - itemNames: Product/Module/Part names - 10 minutes TTL
 * 
 * Activation: Set CACHE_TYPE=redis environment variable (or spring.cache.type=redis)
 * 
 * @since Phase 3 - Performance Optimization (February 4, 2026)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_PRODUCT_MODULES = "productModules";
    public static final String CACHE_MODULE_PARTS = "moduleParts";
    public static final String CACHE_ITEM_NAMES = "itemNames";

    @Value("${life.order-processing.cache.product-modules-ttl-seconds:3600}")
    private long productModulesTtlSeconds;

    @Value("${life.order-processing.cache.module-parts-ttl-seconds:3600}")
    private long modulePartsTtlSeconds;

    @Value("${life.order-processing.cache.masterdata-ttl-seconds:600}")
    private long masterdataTtlSeconds;

    /**
     * Creates Redis cache manager with per-cache TTL configuration.
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        logger.info("Initializing Redis Cache Manager with TTLs: " +
                "productModules={}s, moduleParts={}s, itemNames={}s",
                productModulesTtlSeconds, modulePartsTtlSeconds, masterdataTtlSeconds);

        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(masterdataTtlSeconds))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Per-cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Product modules - BOM data, rarely changes (1 hour TTL)
        cacheConfigurations.put(CACHE_PRODUCT_MODULES, defaultConfig
                .entryTtl(Duration.ofSeconds(productModulesTtlSeconds)));

        // Module parts - BOM data, rarely changes (1 hour TTL)
        cacheConfigurations.put(CACHE_MODULE_PARTS, defaultConfig
                .entryTtl(Duration.ofSeconds(modulePartsTtlSeconds)));

        // Item names - static reference data (10 minute TTL)
        cacheConfigurations.put(CACHE_ITEM_NAMES, defaultConfig
                .entryTtl(Duration.ofSeconds(masterdataTtlSeconds)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
