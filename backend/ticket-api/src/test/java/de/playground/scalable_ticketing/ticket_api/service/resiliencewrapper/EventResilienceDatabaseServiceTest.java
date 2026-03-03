package de.playground.scalable_ticketing.ticket_api.service.resiliencewrapper;

import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventResilienceDatabaseService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventDatabaseService")
class EventResilienceDatabaseServiceTest {

    private static final String EVENT_ID = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    @Mock
    private EventRepository eventRepository;
    @InjectMocks
    private EventResilienceDatabaseService eventResilienceDatabaseService;

    @Test
    @DisplayName("returns the available ticket count when event is found")
    void shouldReturnTicketCountWhenEventFound() {
        // given
        int expectedCount = 50;
        when(eventRepository.findAvailableTicketsById(UUID.fromString(EVENT_ID)))
                .thenReturn(Optional.of(expectedCount));

        // when
        int actualCount = eventResilienceDatabaseService.findAvailableTickets(EVENT_ID);

        // then
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("throws EventNotFoundException when event is not found")
    void shouldThrowExceptionWhenEventNotFound() {
        // given
        when(eventRepository.findAvailableTicketsById(UUID.fromString(EVENT_ID)))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventResilienceDatabaseService.findAvailableTickets(EVENT_ID))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(EVENT_ID);
    }
}
