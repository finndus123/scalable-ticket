package de.playground.scalable_ticketing.ticket_api.service;

import de.playground.scalable_ticketing.common.dto.TicketOrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import static org.mockito.Mockito.verify;


/**
 * Unit tests for {@link EventMessagingService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventMessagingService")
class EventMessagingServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventMessagingService eventMessagingService;

    private static final String EXCHANGE = "test.exchange";
    private static final String ROUTING_KEY = "test.routing.key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventMessagingService, "exchange", EXCHANGE);
        ReflectionTestUtils.setField(eventMessagingService, "routingKey", ROUTING_KEY);
    }

    @Test
    @DisplayName("sends order event via RabbitTemplate")
    void shouldSendOrderEvent() {
        // given
        TicketOrderEvent event = new TicketOrderEvent(
                "reqId", "eventId", "userId", 1, Instant.now().toString()
        );

        // when
        eventMessagingService.sendOrderEvent(event);

        // then
        verify(rabbitTemplate).convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
