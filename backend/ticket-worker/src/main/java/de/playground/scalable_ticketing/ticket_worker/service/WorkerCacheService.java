package de.playground.scalable_ticketing.ticket_worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

/**
 * Service to execute Redis operations for the TicketWorker with Resilience4j patterns.
 * Ensures the worker does not hang or fail if Redis is unavailable.
 */
@Service
public class WorkerCacheService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerCacheService.class);
    private static final String CACHE_KEY_PATTERN = "event:%s:availability";

    private final RedisTemplate<String, Object> redisTemplate;

    public WorkerCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Invalidates the availability cache for the given event ID.
     * Uses CircuitBreaker and TimeLimiter to prevent blocking on slow Redis connections.
     *
     * @param eventId the ID of the event to invalidate
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "fallbackInvalidate")
    @TimeLimiter(name = "redis")
    public void invalidateAvailabilityCache(String eventId) {
        String cacheKey = String.format(CACHE_KEY_PATTERN, eventId);
        redisTemplate.delete(cacheKey);
        logger.info("Invalidated Redis cache for key {}", cacheKey);
    }

    /**
     * Fallback method executed if the CircuitBreaker is open or TimeLimiter triggers.
     * Ensures the worker doesn't fail the message just because the cache is down.
     *
     * @param eventId the event ID
     * @param ex      the exception that triggered the fallback
     */
    public void fallbackInvalidate(String eventId, Throwable ex) {
        logger.warn("Redis unavailable or timeout. Skipping cache invalidation for event {}. Reason: {}", eventId, ex.getMessage());
    }
}
