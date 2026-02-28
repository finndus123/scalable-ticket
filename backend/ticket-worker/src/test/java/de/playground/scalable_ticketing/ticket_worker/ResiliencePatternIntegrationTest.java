package de.playground.scalable_ticketing.ticket_worker;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.common.exception.UserNotFoundException;
import de.playground.scalable_ticketing.ticket_worker.config.TestWorkerInfrastructureConfig;
import de.playground.scalable_ticketing.ticket_worker.service.WorkerCacheService;
import de.playground.scalable_ticketing.ticket_worker.service.WorkerDatabaseService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestWorkerInfrastructureConfig.class)
@ContextConfiguration(initializers = TestWorkerInfrastructureConfig.RedisInitializer.class)
@DisplayName("Resilience Pattern Integration Tests (Worker)")
class ResiliencePatternIntegrationTest {

    private static final String NON_EXISTENT_ID = "00000000-0000-0000-0000-000000000000";
    private static final String EXISTING_EVENT_ID = "e58ed763-928c-4155-bee9-fdbaaadc15f3";

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoSpyBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WorkerDatabaseService workerDatabaseService;

    @Autowired
    private WorkerCacheService workerCacheService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Nested
    @DisplayName("Database Circuit Breaker - EventNotFoundException handling")
    class DatabaseCircuitBreakerIgnoreExceptions {
        @Test
        @DisplayName("EventNotFoundException does NOT open the database circuit breaker")
        void eventNotFoundExceptionShouldNotOpenCircuitBreaker() {
            when(eventRepository.findById(UUID.fromString(NON_EXISTENT_ID)))
                    .thenReturn(Optional.empty());

            CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

            int callCount = 20;
            for (int i = 0; i < callCount; i++) {
                try {
                    workerDatabaseService.getEventOrThrow(UUID.fromString(NON_EXISTENT_ID));
                    workerDatabaseService.getUserOrThrow(UUID.fromString(NON_EXISTENT_ID));
                } catch (EventNotFoundException | UserNotFoundException ignored) {
                }
            }

            assertThat(databaseCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(databaseCb.getMetrics().getNumberOfFailedCalls()).isZero();
        }
    }

    @Nested
    @DisplayName("Database Circuit Breaker - Infrastructure Failures")
    class DatabaseCircuitBreakerInfrastructureFailures {
        @Test
        @DisplayName("RuntimeException opens the database circuit breaker")
        void runtimeExceptionShouldOpenCircuitBreaker() {
            when(eventRepository.findById(any(UUID.class)))
                    .thenThrow(new RuntimeException("Database down"));

            CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

            int callCount = 20;
            for (int i = 0; i < callCount; i++) {
                try {
                    workerDatabaseService.getEventOrThrow(UUID.fromString(EXISTING_EVENT_ID));
                } catch (RuntimeException ignored) {
                }
            }

            assertThat(databaseCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    @Nested
    @DisplayName("Database Bulkhead - Concurrent requests")
    class DatabaseBulkheadTests {
        @Test
        @DisplayName("Exceeding max concurrent calls triggers BulkheadFullException")
        void exceedingMaxConcurrentCallsShouldTriggerBulkheadFullException() throws InterruptedException {
            int threadCount = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            Event mockEvent = mock(Event.class);
            when(eventRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
                Thread.sleep(500);
                return Optional.of(mockEvent);
            });

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Future<Event>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    startLatch.await();
                    try {
                        return workerDatabaseService.getEventOrThrow(UUID.fromString(EXISTING_EVENT_ID));
                    } finally {
                        finishLatch.countDown();
                    }
                }));
            }

            startLatch.countDown();
            finishLatch.await();
            executorService.shutdown();

            long failedWithBulkhead = futures.stream().filter(f -> {
                try {
                    f.get();
                    return false;
                } catch (ExecutionException e) {
                    return e.getCause() instanceof BulkheadFullException;
                } catch (InterruptedException e) {
                    return false;
                }
            }).count();

            assertThat(failedWithBulkhead).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Redis Circuit Breaker & Fallback")
    class RedisFallbackTests {
        @Test
        @DisplayName("Cache invalidation fails gracefully when Redis throws an exception")
        void redisFailureShouldTriggerFallback() {
            doThrow(new RedisConnectionFailureException("Redis down"))
                    .when(redisTemplate).delete(anyString());

            CircuitBreaker redisCb = circuitBreakerRegistry.circuitBreaker("redis");

            // Verify that calling invalidateAvailabilityCache does NOT throw an exception
            assertThatCode(() -> {
                workerCacheService.invalidateAvailabilityCache(EXISTING_EVENT_ID);
            }).doesNotThrowAnyException();

            // To prove that the circuit breaker records the failure and opens (window size is 5)
            for (int i = 0; i < 6; i++) {
                workerCacheService.invalidateAvailabilityCache(EXISTING_EVENT_ID);
            }

            assertThat(redisCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Should still not throw exception when OPEN due to fallback
            assertThatCode(() -> {
                workerCacheService.invalidateAvailabilityCache(EXISTING_EVENT_ID);
            }).doesNotThrowAnyException();
        }
    }
}
