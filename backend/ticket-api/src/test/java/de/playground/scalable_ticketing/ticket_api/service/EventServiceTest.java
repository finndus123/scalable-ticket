package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventService}.
 *
 * All infrastructure dependencies are replaced by Mockito mocks for the delegated service classes ({@link EventCacheService}, {@link EventDatabaseService}, {@link EventMessagingService}).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String EVENT_ID   = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    private static final String USER_ID    = "7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a";
    private static final String REQUEST_ID = "b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e";

    // -------------------------------------------------------------------------
    // Mocks & SUT
    // -------------------------------------------------------------------------

    @Mock
    private EventCacheService eventCacheService;

    @Mock
    private EventDatabaseService eventDatabaseService;

    @Mock
    private EventMessagingService eventMessagingService;

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
            when(eventCacheService.getAvailabilityCountFromCache(EVENT_ID)).thenReturn(Optional.of(cachedCount));

            // when
            TicketAvailabilityResponse response = eventService.getAvailabilityCount(EVENT_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.eventId()).isEqualTo(EVENT_ID);
            assertThat(response.availableTickets()).isEqualTo(cachedCount);

            verify(eventDatabaseService, never()).findAvailableTickets(anyString());
        }

        @Test
        @DisplayName("falls back to database and populates cache on a cache miss")
        void shouldFallBackToDatabaseAndPopulateCacheOnCacheMiss() {
            // given
            int dbCount = 100;
            when(eventCacheService.getAvailabilityCountFromCache(EVENT_ID))
                    .thenReturn(Optional.empty());
            when(eventDatabaseService.findAvailableTickets(EVENT_ID))
                    .thenReturn(dbCount);

            // when
            TicketAvailabilityResponse response = eventService.getAvailabilityCount(EVENT_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.eventId()).isEqualTo(EVENT_ID);
            assertThat(response.availableTickets()).isEqualTo(dbCount);

            verify(eventDatabaseService).findAvailableTickets(EVENT_ID);
            verify(eventCacheService).writeAvailabilityCountToCache(eq(EVENT_ID), eq(dbCount));
        }

        @Test
        @DisplayName("throws EventNotFoundException when event is absent from both cache and database")
        void shouldThrowEventNotFoundExceptionWhenEventDoesNotExist() {
            // given
            when(eventCacheService.getAvailabilityCountFromCache(EVENT_ID))
                    .thenReturn(Optional.empty());
            when(eventDatabaseService.findAvailableTickets(EVENT_ID))
                    .thenThrow(new EventNotFoundException(EVENT_ID));

            // when / then
            assertThatThrownBy(() -> eventService.getAvailabilityCount(EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining(EVENT_ID);

            verify(eventDatabaseService).findAvailableTickets(EVENT_ID);
            verify(eventCacheService, never()).writeAvailabilityCountToCache(anyString(), anyInt());
        }
    }

    // =========================================================================
    // createOrder
    // =========================================================================

    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        @Test
        @DisplayName("published event contains the correct eventId, userId, and quantity")
        void shouldPublishEventWithCorrectFields() {
            // given
            int quantity = 3;
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, quantity, REQUEST_ID);
            ArgumentCaptor<TicketOrderEvent> eventCaptor = ArgumentCaptor.forClass(TicketOrderEvent.class);

            // when
            eventService.createOrder(EVENT_ID, request);

            // then
            verify(eventMessagingService).sendOrderEvent(eventCaptor.capture());

            TicketOrderEvent published = eventCaptor.getValue();
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
            ArgumentCaptor<TicketOrderEvent> eventCaptor = ArgumentCaptor.forClass(TicketOrderEvent.class);

            // when
            eventService.createOrder(EVENT_ID, request);

            // then
            verify(eventMessagingService).sendOrderEvent(eventCaptor.capture());

            TicketOrderEvent published = eventCaptor.getValue();
            assertThat(published.requestId()).isNull();
        }
    }
}
