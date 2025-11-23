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
 * Uses in-memory caching for user data only
 * Stall availability and reservation status are not cached to ensure real-time accuracy
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USER_CACHE = "users";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache(USER_CACHE)
        ));
        return cacheManager;
    }
}
