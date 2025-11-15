package exhibitflow.reservation_service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for frequently accessed data
 * Uses in-memory caching with Caffeine for better performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_CACHE = "users";
    public static final String STALL_CACHE = "stalls";
    public static final String RESERVATION_CACHE = "reservations";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache(USER_CACHE),
            new ConcurrentMapCache(STALL_CACHE),
            new ConcurrentMapCache(RESERVATION_CACHE)
        ));
        return cacheManager;
    }
}
