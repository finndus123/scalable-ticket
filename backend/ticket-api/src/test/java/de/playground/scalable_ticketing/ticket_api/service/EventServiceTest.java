package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventService}.
 *
 * All infrastructure dependencies (Redis, RabbitMQ, DB) are replaced by Mockito mocks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String EVENT_ID    = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    private static final String USER_ID     = "7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a";
    private static final String REQUEST_ID  = "b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e";
    private static final String CACHE_KEY   = "event:" + EVENT_ID + ":availability";

    private static final String TEST_EXCHANGE    = "test.exchange";
    private static final String TEST_ROUTING_KEY = "test.routing.key";

    // -------------------------------------------------------------------------
    // Mocks & SUT
    // -------------------------------------------------------------------------

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventService eventService;

    // =========================================================================
    // getAvailabilityCount
    // =========================================================================

    @Nested
    @DisplayName("getAvailabilityCount()")
    class GetAvailabilityCount {

        @Test
        @DisplayName("returns cached value and skips the database on a cache hit")
        void shouldReturnCachedValueOnCacheHit() {
            // given
            int cachedCount = 42;
            when(valueOperations.get(CACHE_KEY)).thenReturn(cachedCount);

            // when
            TicketAvailabilityResponse response = eventService.getAvailabilityCount(EVENT_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.eventId()).isEqualTo(EVENT_ID);
            assertThat(response.availableTickets()).isEqualTo(cachedCount);

            verify(eventRepository, never()).findById(anyString());
            verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("falls back to database and populates cache on a cache miss")
        void shouldFallBackToDatabaseAndPopulateCacheOnCacheMiss() {
            // given
            int dbCount = 100;
            Event event = new Event(EVENT_ID, "Test Event", 200, dbCount);

            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            // when
            TicketAvailabilityResponse response = eventService.getAvailabilityCount(EVENT_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.eventId()).isEqualTo(EVENT_ID);
            assertThat(response.availableTickets()).isEqualTo(dbCount);

            verify(eventRepository).findById(EVENT_ID);
            verify(valueOperations).set(eq(CACHE_KEY), eq(dbCount), any(Duration.class));
        }

        @Test
        @DisplayName("throws EventNotFoundException when event is absent from both cache and database")
        void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
            // given
            when(valueOperations.get(CACHE_KEY)).thenReturn(null);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.getAvailabilityCount(EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining(EVENT_ID);

            verify(eventRepository).findById(EVENT_ID);
            verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("cache key is built from the eventId (format: event:{id}:availability)")
        void shouldUseCorrectCacheKeyFormat() {
            // given
            when(valueOperations.get(CACHE_KEY)).thenReturn(10);

            // when
            eventService.getAvailabilityCount(EVENT_ID);

            // then – the exact cache key must be used
            verify(valueOperations).get(CACHE_KEY);
        }
    }

    // =========================================================================
    // createOrder
    // =========================================================================

    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        @BeforeEach
        void setUp() {
            // Inject private @Value fields of Service that are not set by @InjectMocks
            ReflectionTestUtils.setField(eventService, "exchange",   TEST_EXCHANGE);
            ReflectionTestUtils.setField(eventService, "routingKey", TEST_ROUTING_KEY);
        }

        @Test
        @DisplayName("published event contains the correct eventId, userId, and quantity to the configured exchange and routing key")
        void shouldPublishEventWithCorrectFields() {
            // given
            int quantity = 3;
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, quantity, REQUEST_ID);

            ArgumentCaptor<TicketOrderEvent> messageCaptor = ArgumentCaptor.forClass(TicketOrderEvent.class);

            // when
            eventService.createOrder(EVENT_ID, request);

            // then
            verify(rabbitTemplate).convertAndSend(
                    eq(TEST_EXCHANGE),
                    eq(TEST_ROUTING_KEY),
                    messageCaptor.capture()
            );

            TicketOrderEvent published = messageCaptor.getValue();


            assertThat(published.eventId()).isEqualTo(EVENT_ID);
            assertThat(published.userId()).isEqualTo(USER_ID);
            assertThat(published.quantity()).isEqualTo(quantity);
            assertThat(published.requestId()).isEqualTo(REQUEST_ID);
            assertThat(published.timestamp()).isNotBlank();
        }

        @Test
        @DisplayName("publishes event with null requestId when it is absent in the request")
        void shouldPublishEventWithNullRequestIdWhenAbsent() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 1, null);
            ArgumentCaptor<TicketOrderEvent> messageCaptor = ArgumentCaptor.forClass(TicketOrderEvent.class);

            // when
            eventService.createOrder(EVENT_ID, request);

            // then
            verify(rabbitTemplate).convertAndSend(
                    eq(TEST_EXCHANGE),
                    eq(TEST_ROUTING_KEY),
                    messageCaptor.capture()
            );

            TicketOrderEvent published = messageCaptor.getValue();

            assertThat(published.requestId()).isNull();
        }

    }
}
