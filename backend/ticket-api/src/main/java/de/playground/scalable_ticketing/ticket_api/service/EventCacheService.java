package de.playground.scalable_ticketing.ticket_api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service encapsulating all Redis cache interactions for event data.
 * <p>
 * Separated from {@link EventService} so that resilience4j annotations are applied via Spring AOP proxy (avoids the self-invocation problem).
 * Circuit-breaker instance: "redis"
 */
@Service
public class EventCacheService {

    private static final String CACHE_KEY_FORMAT = "event:%s:availability";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private static final Logger logger = LoggerFactory.getLogger(EventCacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public EventCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns the availability count from Redis cache.
     * Falls back to {@link #redisReadFallback} when the circuit breaker is open
     * or a Redis error occurs.
     *
     * @param eventId the unique identifier of the event
     * @return Optional containing the cached availability count, or empty on miss / error
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "redisReadFallback")
    public Optional<Integer> getAvailabilityCountFromCache(String eventId) {
        final String cacheKey = String.format(CACHE_KEY_FORMAT, eventId);
        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                int availabilityCount = (Integer) cachedValue;
                return Optional.of(availabilityCount);
            }
        } catch (Exception e) {
            logger.error("Error reading from Redis cache for key: {}.", cacheKey, e);
        }
        return Optional.empty();
    }

    /**
     * Writes the availability count to the Redis cache with a fixed TTL.
     * Falls back to {@link #redisWriteFallback} when the circuit breaker is open
     * or a Redis error occurs.
     *
     * @param eventId the unique identifier of the event
     * @param count   the availability count to cache
     */
    @CircuitBreaker(name = "redis", fallbackMethod = "redisWriteFallback")
    public void writeAvailabilityCountToCache(String eventId, int count) {
        final String cacheKey = String.format(CACHE_KEY_FORMAT, eventId);
        try {
            redisTemplate.opsForValue().set(cacheKey, count, CACHE_TTL);
        } catch (Exception e) {
            logger.error("Error writing to Redis cache for key: {}", cacheKey, e);
        }
    }

    // -------------------------------------------------------------------------
    // Fallback methods
    // -------------------------------------------------------------------------

    /**
     * Fallback for {@link #getAvailabilityCountFromCache}.
     * Returns an empty Optional so the caller can fall back to the database.
     */
    public Optional<Integer> redisReadFallback(String eventId, Exception e) {
        logger.warn("Redis read failed for event {}, triggering DB fallback. Reason: {}", eventId, e.getMessage());
        return Optional.empty();
    }

    /**
     * Fallback for {@link #writeAvailabilityCountToCache}.
     * Logs the error; the database remains the source of truth.
     */
    public void redisWriteFallback(String eventId, int count, Exception e) {
        logger.error("Failed to write to Redis for event {}. DB is still correct. Reason: {}", eventId, e.getMessage());
    }
}
