// package de.playground.scalable_ticketing.ticket_api;

// import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
// import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
// import de.playground.scalable_ticketing.ticket_api.service.EventDatabaseService;
// import io.github.resilience4j.circuitbreaker.CircuitBreaker;
// import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;

// import java.util.Optional;
// import java.util.UUID;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.when;

// Todo fix
// /**
//  * Integration test verifying that resilience4j patterns (circuit breakers, bulkheads, retries) are correctly wired via Spring AOP proxies.
//  * 
//  * Uses a full Spring ApplicationContext with mocked infrastructure beans (Redis, RabbitMQ, DB).
//  * The auto-configurations for real infrastructure are excluded via {@code spring.autoconfigure.exclude}
//  * so no actual connections are required.
//  * 
//  * The focus is on {@link EventNotFoundException} does <strong>not</strong> trigger the database circuit breaker, will be expanded in the future.
//  */
// @SpringBootTest(properties = {
//         "spring.autoconfigure.exclude=" +
//                 "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
//                 "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
//                 "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
//                 "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
// })
// @DisplayName("Resilience Pattern Integration Tests")
// class ResiliencePatternIntegrationTest {

//     // -------------------------------------------------------------------------
//     // Constants
//     // -------------------------------------------------------------------------

//     private static final String NON_EXISTENT_EVENT_ID = "00000000-0000-0000-0000-000000000000";
//     private static final String EXISTING_EVENT_ID     = "e58ed763-928c-4155-bee9-fdbaaadc15f3";

//     // -------------------------------------------------------------------------
//     // Mocked infrastructure beans
//     // -------------------------------------------------------------------------

//     @MockitoBean
//     private EventRepository eventRepository;

//     /** Mocking the connection factory prevents Redis auto-config from opening a real socket. */
//     @MockitoBean
//     private RedisConnectionFactory redisConnectionFactory;

//     @MockitoBean
//     private RedisTemplate<String, Object> redisTemplate;

//     @MockitoBean
//     private RabbitTemplate rabbitTemplate;

//     // -------------------------------------------------------------------------
//     // Real beans under test
//     // -------------------------------------------------------------------------

//     @Autowired
//     private EventDatabaseService eventDatabaseService;

//     @Autowired
//     private CircuitBreakerRegistry circuitBreakerRegistry;

//     // -------------------------------------------------------------------------
//     // Setup
//     // -------------------------------------------------------------------------

//     @BeforeEach
//     void resetCircuitBreakers() {
//         circuitBreakerRegistry.getAllCircuitBreakers()
//                 .forEach(CircuitBreaker::reset);
//     }

//     // =========================================================================
//     // Database Circuit Breaker – ignoreExceptions
//     // =========================================================================

//     @Nested
//     @DisplayName("Database Circuit Breaker – EventNotFoundException handling")
//     class DatabaseCircuitBreakerIgnoreExceptions {

//         /**
//          * Critical test: EventNotFoundException is configured as an ignored exception.
//          * Even when thrown more times than the sliding window size, the circuit breaker
//          * must remain CLOSED.
//          */
//         @Test
//         @DisplayName("EventNotFoundException does NOT open the database circuit breaker")
//         void eventNotFoundExceptionShouldNotOpenCircuitBreaker() {
//             // given – repository returns empty for a non-existent event
//             when(eventRepository.findAvailableTicketsById(UUID.fromString(NON_EXISTENT_EVENT_ID)))
//                     .thenReturn(Optional.empty());

//             CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

//             // when – call more times than the slidingWindowSize (5 in test config)
//             int callCount = 10;
//             for (int i = 0; i < callCount; i++) {
//                 try {
//                     eventDatabaseService.findAvailableTickets(NON_EXISTENT_EVENT_ID);
//                 } catch (EventNotFoundException ignored) {
//                     // Expected – event does not exist, but this must not trip the circuit breaker
//                 }
//             }

//             // then – circuit breaker must still be CLOSED
//             assertThat(databaseCb.getState())
//                     .as("Database circuit breaker should remain CLOSED after EventNotFoundException")
//                     .isEqualTo(CircuitBreaker.State.CLOSED);

//             // Additional: verify no failures were recorded
//             assertThat(databaseCb.getMetrics().getNumberOfFailedCalls())
//                     .as("No failed calls should be recorded for ignored exceptions")
//                     .isZero();
//         }
//     }

//     // =========================================================================
//     // Database Circuit Breaker – real infrastructure failures
//     // =========================================================================

//     @Nested
//     @DisplayName("Database Circuit Breaker – infrastructure failures")
//     class DatabaseCircuitBreakerInfrastructureFailures {

//         /**
//          * When real infrastructure exceptions (e.g. database unavailable) occur,
//          * the circuit breaker must eventually OPEN to protect the system.
//          */
//         @Test
//         @DisplayName("RuntimeException opens the database circuit breaker after exceeding failure threshold")
//         void runtimeExceptionShouldOpenCircuitBreaker() {
//             // given – simulate a database failure
//             when(eventRepository.findAvailableTicketsById(any(UUID.class)))
//                     .thenThrow(new RuntimeException("Database connection refused"));

//             CircuitBreaker databaseCb = circuitBreakerRegistry.circuitBreaker("database");

//             // when – call enough times to fill the sliding window (size=5) and exceed threshold (50%)
//             int callCount = 6;
//             for (int i = 0; i < callCount; i++) {
//                 try {
//                     eventDatabaseService.findAvailableTickets(EXISTING_EVENT_ID);
//                 } catch (RuntimeException ignored) {
//                     // Expected – infrastructure failure
//                 }
//             }

//             // then – circuit breaker must be OPEN
//             assertThat(databaseCb.getState())
//                     .as("Database circuit breaker should be OPEN after repeated infrastructure failures")
//                     .isEqualTo(CircuitBreaker.State.OPEN);
//         }
//     }

//     // Todo: Add tests for other circuit breaker, bulkhead and retry patterns
// }
