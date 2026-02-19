package de.playground.scalable_ticketing.ticket_api.util;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for utility class {@link TicketOrderRequestMapper}.
 */
@DisplayName("TicketOrderRequestMapper")
class TicketOrderRequestMapperTest {

    private static final String EVENT_ID    = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    private static final String USER_ID     = "7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a";
    private static final String REQUEST_ID  = "b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e";

    @Nested
    @DisplayName("toEvent()")
    class ToEvent {

        @Test
        @DisplayName("maps eventId from the path variable, not from the request body")
        void shouldUseEventIdFromPathVariable() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 2, REQUEST_ID);

            // when
            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);

            // then
            assertThat(event.eventId()).isEqualTo(EVENT_ID);
        }

        @Test
        @DisplayName("maps userId from the order request")
        void shouldMapUserIdFromRequest() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 2, REQUEST_ID);

            // when
            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);

            // then
            assertThat(event.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("maps quantity from the order request")
        void shouldMapQuantityFromRequest() {
            // given
            int expectedQuantity = 5;
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, expectedQuantity, REQUEST_ID);

            // when
            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);

            // then
            assertThat(event.quantity()).isEqualTo(expectedQuantity);
        }

        @Test
        @DisplayName("maps requestId from the order request")
        void shouldMapRequestIdFromRequest() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 2, REQUEST_ID);

            // when
            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);

            // then
            assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        }

        @Test
        @DisplayName("sets timestamp as a valid ISO-8601 instant string")
        void shouldSetTimestampAsValidIsoInstant() {
            // given
            Instant before = Instant.now();
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 2, REQUEST_ID);

            // when
            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);
            Instant after = Instant.now();

            // then – timestamp must be parseable and fall within the test window
            Instant timestamp = Instant.parse(event.timestamp());
            assertThat(timestamp)
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("maps null requestId without throwing")
        void shouldAllowNullRequestId() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 1, null);

            // when / then
            assertThatCode(() -> TicketOrderRequestMapper.toEvent(EVENT_ID, request))
                    .doesNotThrowAnyException();

            TicketOrderEvent event = TicketOrderRequestMapper.toEvent(EVENT_ID, request);
            assertThat(event.requestId()).isNull();
        }

        @Test
        @DisplayName("returns a new TicketOrderEvent instance on every call")
        void shouldReturnNewInstanceOnEveryCall() {
            // given
            TicketOrderRequest request = new TicketOrderRequest(USER_ID, 2, REQUEST_ID);

            // when
            TicketOrderEvent first  = TicketOrderRequestMapper.toEvent(EVENT_ID, request);
            TicketOrderEvent second = TicketOrderRequestMapper.toEvent(EVENT_ID, request);

            // then
            assertThat(first).isNotSameAs(second);
        }
    }
}
