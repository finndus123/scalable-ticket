package de.playground.scalable_ticketing.ticket_worker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerCacheService Unit Tests")
class WorkerCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private WorkerCacheService workerCacheService;

    @Test
    @DisplayName("Should invalidate availability cache using the correct key pattern")
    void shouldInvalidateAvailabilityCache() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String expectedCacheKey = "event:" + eventId + ":availability";

        // Act
        workerCacheService.invalidateAvailabilityCache(eventId);

        // Assert
        verify(redisTemplate).delete(expectedCacheKey);
    }

    @Test
    @DisplayName("Fallback method should log warning and not throw exceptions")
    void fallbackShouldExecuteWithoutThrowing() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        RuntimeException dummyException = new RuntimeException("Redis connection timeout");

        // Act & Assert
        assertThatCode(() -> workerCacheService.fallbackInvalidate(eventId, dummyException))
                .doesNotThrowAnyException();
    }
}
