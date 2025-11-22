package exhibitflow.reservation_service.consumer;

import exhibitflow.reservation_service.dto.StallCreatedEvent;
import exhibitflow.reservation_service.dto.StallUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for stall-related events from Stall Service
 * Handles cache invalidation when stalls are created or updated
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StallEventConsumer {

    private final CacheManager cacheManager;

    /**
     * Handle stall created events
     * Clears the stalls cache to ensure fresh data on next fetch
     */
    @KafkaListener(
        topics = "${kafka.topics.stall-created}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStallCreated(
            @Payload StallCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        
        log.info("Received StallCreatedEvent from topic: {} | Offset: {} | StallId: {} | StallCode: {}",
                topic, offset, event.getStallId(), event.getStallCode());
        
        try {
            // Clear stalls cache to ensure venue map fetches latest data
            clearStallsCache();
            
            log.info("Successfully processed StallCreatedEvent for stall: {} ({})",
                    event.getStallCode(), event.getStallId());
                    
        } catch (Exception e) {
            log.error("Error processing StallCreatedEvent for stall: {} | Error: {}",
                    event.getStallId(), e.getMessage(), e);
            // In production, consider implementing retry logic or DLQ
        }
    }

    /**
     * Handle stall updated events
     * Clears the stalls cache to ensure fresh data on next fetch
     */
    @KafkaListener(
        topics = "${kafka.topics.stall-updated}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStallUpdated(
            @Payload StallUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        
        log.info("Received StallUpdatedEvent from topic: {} | Offset: {} | StallId: {} | StallCode: {}",
                topic, offset, event.getStallId(), event.getStallCode());
        
        try {
            // Clear specific stall from cache
            var stallsCache = cacheManager.getCache("stalls");
            if (stallsCache != null) {
                stallsCache.evict(event.getStallId());
                log.debug("Evicted stall {} from cache", event.getStallId());
            }
            
            log.info("Successfully processed StallUpdatedEvent for stall: {} ({})",
                    event.getStallCode(), event.getStallId());
                    
        } catch (Exception e) {
            log.error("Error processing StallUpdatedEvent for stall: {} | Error: {}",
                    event.getStallId(), e.getMessage(), e);
        }
    }

    /**
     * Clear all stalls from cache
     */
    private void clearStallsCache() {
        var stallsCache = cacheManager.getCache("stalls");
        if (stallsCache != null) {
            stallsCache.clear();
            log.debug("Cleared stalls cache");
        }
    }
}
