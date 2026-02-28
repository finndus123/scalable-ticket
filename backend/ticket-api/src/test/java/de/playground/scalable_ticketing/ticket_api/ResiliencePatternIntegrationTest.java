package de.playground.scalable_ticketing.ticket_api;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.ticket_api.config.TestApiInfrastructureConfig;
import de.playground.scalable_ticketing.ticket_api.service.EventDatabaseService;
import de.playground.scalable_ticketing.ticket_api.service.EventMessagingService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test verifying that resilience4j patterns (circuit breakers, bulkheads, retries) are correctly wired via Spring AOP proxies.
 * <p>
 * The focus is on {@link EventNotFoundException} does <strong>not</strong> trigger the database circuit breaker, will be expanded in the future.
 */
@SpringBootTest
@Import(TestApiInfrastructureConfig.class)
@ContextConfiguration(initializers = TestApiInfrastructureConfig.RedisInitializer.class)
@DisplayName("Resilience Pattern Integration Tests")
class ResiliencePatternIntegrationTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String NON_EXISTENT_EVENT_ID = "00000000-0000-0000-0000-000000000000";
    private static final String EXISTING_EVENT_ID = "e58ed763-928c-4155-bee9-fdbaaadc15f3";

    // -------------------------------------------------------------------------
    // Mocked infrastructure beans
    // -------------------------------------------------------------------------

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    // -------------------------------------------------------------------------
    // Real beans under test
    // -------------------------------------------------------------------------

    @Autowired
    private EventDatabaseService eventDatabaseService;

    @Autowired
    private EventMessagingService eventMessagingService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);
    }

    // =========================================================================
    // Database Circuit Breaker – ignore custom Exceptions
    // =========================================================================

    @Nested
    @DisplayName("Database Circuit Breaker – EventNotFoundException handling")
    class DatabaseCircuitBreakerIgnoreExceptions {

        /**
         * Critical test: EventNotFoundException is configured as an ignored exception.
         * Even when thrown more times than the sliding window size, the circuit breaker
         * must remain CLOSED.
         */
        @Test
        @DisplayName("EventNotFoundException does NOT open the database circuit breaker")
        void eventNotFoundExceptionShouldNotOpenCircuitBreaker() {
            // given – repository returns empty for a non-existent event
            when(eventRepository.findAvailableTicketsById(UUID.fromString(NON_EXISTENT_EVENT_ID)))
                    .thenReturn(Optional.empty());

            CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

            // when – call more times than the slidingWindowSize (5 in test config)
            int callCount = 10;
            for (int i = 0; i < callCount; i++) {
                try {
                    eventDatabaseService.findAvailableTickets(NON_EXISTENT_EVENT_ID);
                } catch (EventNotFoundException ignored) {
                    // Expected – event does not exist, but this must not trip the circuit breaker
                }
            }

            // then – circuit breaker must still be CLOSED
            assertThat(databaseCb.getState())
                    .as("Database circuit breaker should remain CLOSED after EventNotFoundException")
                    .isEqualTo(CircuitBreaker.State.CLOSED);

            // Additional: verify no failures were recorded
            assertThat(databaseCb.getMetrics().getNumberOfFailedCalls())
                    .as("No failed calls should be recorded for ignored exceptions")
                    .isZero();
        }
    }

    // =========================================================================
    // Database Circuit Breaker – real infrastructure failures
    // =========================================================================

    @Nested
    @DisplayName("Database Circuit Breaker – infrastructure failures")
    class DatabaseCircuitBreakerInfrastructureFailures {

        /**
         * When real infrastructure exceptions (e.g. database unavailable) occur,
         * the circuit breaker must eventually OPEN to protect the system.
         */
        @Test
        @DisplayName("RuntimeException opens the database circuit breaker after exceeding failure threshold")
        void runtimeExceptionShouldOpenCircuitBreaker() {
            // given – simulate a database failure
            when(eventRepository.findAvailableTicketsById(any(UUID.class)))
                    .thenThrow(new RuntimeException("Database connection refused"));

            CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

            // when – call enough times to fill the sliding window (size=5) and exceed threshold (50%)
            int callCount = 6;
            for (int i = 0; i < callCount; i++) {
                try {
                    eventDatabaseService.findAvailableTickets(EXISTING_EVENT_ID);
                } catch (RuntimeException ignored) {
                    // Expected – infrastructure failure
                }
            }

            // then – circuit breaker must be OPEN
            assertThat(databaseCb.getState())
                    .as("Database circuit breaker should be OPEN after repeated infrastructure failures")
                    .isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    // =========================================================================
    // Database Bulkhead
    // =========================================================================

    @Nested
    @DisplayName("Database Bulkhead – concurrent requests")
    class DatabaseBulkheadTests {

        @Test
        @DisplayName("Exceeding max concurrent calls triggers BulkheadFullException")
        void exceedingMaxConcurrentCallsShouldTriggerBulkheadFullException() throws InterruptedException {
            int threadCount = 50;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            // given – slow database simulation
            when(eventRepository.findAvailableTicketsById(any(UUID.class))).thenAnswer(invocation -> {
                Thread.sleep(500);
                return Optional.of(100);
            });

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Future<Integer>> futures = new ArrayList<>();

            // when – submit multiple concurrent requests
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    startLatch.await(); // wait for all threads to start at the same time
                    try {
                        return eventDatabaseService.findAvailableTickets(EXISTING_EVENT_ID);
                    } finally {
                        finishLatch.countDown();
                    }
                }));
            }

            startLatch.countDown(); // unblock all threads
            finishLatch.await();    // wait for all to finish

            executorService.shutdown();

            // then – at least one request must have failed with BulkheadFullException
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

            assertThat(failedWithBulkhead)
                    .as("Expected some calls to fail with BulkheadFullException because max concurrent calls were exceeded")
                    .isGreaterThan(0);
        }
    }

    // =========================================================================
    // RabbitMQ Retry
    // =========================================================================

    @Nested
    @DisplayName("RabbitMQ Retry – retry on failure")
    class RabbitMqRetryTests {

        @Test
        @DisplayName("AmqpException triggers retry and eventually fails if max attempts exceeded")
        void maxRetriesExceededShouldThrow() {
            // given – publishing always fails with AmqpException
            AmqpException exception = new AmqpException("RabbitMQ connection down") {
            };
            doThrow(exception).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            TicketOrderEvent event = new TicketOrderEvent("req-1", EXISTING_EVENT_ID, "user-1", 2, Instant.now().toString());

            // when / then
            assertThatThrownBy(() -> eventMessagingService.sendOrderEvent(event))
                    .isInstanceOf(AmqpException.class);

            // verify that it was retried
            verify(rabbitTemplate, atLeast(2)).convertAndSend(anyString(), anyString(), eq(event));
        }

        @Test
        @DisplayName("AmqpException triggers retry and succeeds on subsequent try")
        void retrySucceedsOnSubsequentTry() {
            // given – publishing fails once, then succeeds
            AmqpException exception = new AmqpException("Transient network glitch") {
            };
            doThrow(exception).doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            TicketOrderEvent event = new TicketOrderEvent("req-2", EXISTING_EVENT_ID, "user-2", 1, Instant.now().toString());

            // when 
            eventMessagingService.sendOrderEvent(event);

            // then – no exception thrown, and rabbitTemplate was called exactly twice
            verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), eq(event));
        }
    }
}
