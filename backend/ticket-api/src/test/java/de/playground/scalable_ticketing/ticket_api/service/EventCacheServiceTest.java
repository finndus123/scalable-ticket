package de.playground.scalable_ticketing.ticket_api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventCacheService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventCacheService")
class EventCacheServiceTest {

    private static final String EVENT_ID = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    private static final String CACHE_KEY = "event:" + EVENT_ID + ":availability";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EventCacheService eventCacheService;

    @Nested
    @DisplayName("getAvailabilityCountFromCache")
    class GetAvailabilityCountFromCache {

        @Test
        @DisplayName("returns optional with count on cache hit")
        void shouldReturnCountOnHit() {
            int count = 42;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(count);

            Optional<Integer> result = eventCacheService.getAvailabilityCountFromCache(EVENT_ID);

            assertThat(result).isPresent().contains(count);
        }

        @Test
        @DisplayName("returns empty optional on cache miss")
        void shouldReturnEmptyOnMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);

            Optional<Integer> result = eventCacheService.getAvailabilityCountFromCache(EVENT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty optional on Redis exception")
        void shouldReturnEmptyOnException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

            Optional<Integer> result = eventCacheService.getAvailabilityCountFromCache(EVENT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("writeAvailabilityCountToCache")
    class WriteAvailabilityCountToCache {

        @Test
        @DisplayName("writes count to redis with TTL")
        void shouldWriteToRedis() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            int count = 100;

            eventCacheService.writeAvailabilityCountToCache(EVENT_ID, count);

            verify(valueOperations).set(eq(CACHE_KEY), eq(count), any(Duration.class));
        }

        @Test
        @DisplayName("handles exception without throwing")
        void shouldHandleException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RuntimeException("Redis error")).when(valueOperations)
                    .set(anyString(), any(), any());

            // Should not throw
            eventCacheService.writeAvailabilityCountToCache(EVENT_ID, 100);
        }
    }

    @Nested
    @DisplayName("Fallbacks")
    class Fallbacks {

        @Test
        @DisplayName("redisReadFallback returns empty Optional")
        void redisReadFallback() {
            Optional<Integer> result = eventCacheService.redisReadFallback(EVENT_ID, new RuntimeException("test"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("redisWriteFallback does not throw")
        void redisWriteFallback() {
            // Should not throw
            eventCacheService.redisWriteFallback(EVENT_ID, 100, new RuntimeException("test"));
        }
    }
}
