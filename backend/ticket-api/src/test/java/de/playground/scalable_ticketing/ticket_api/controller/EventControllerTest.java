package de.playground.scalable_ticketing.ticket_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import de.playground.scalable_ticketing.common.exception.EventNotFoundException;
import de.playground.scalable_ticketing.ticket_api.dto.TicketAvailabilityResponse;
import de.playground.scalable_ticketing.ticket_api.dto.TicketOrderRequest;
import de.playground.scalable_ticketing.ticket_api.exception.GlobalExceptionHandler;
import de.playground.scalable_ticketing.ticket_api.service.EventService;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link EventController} using Spring WebMVC test
 *
 * Only the web layer is loaded for this test.
 * Tests include: Basic functionality of handing requests to Service, validation of requests and error handling.
 * {@link EventService} is replaced by a Mockito mock so no infrastructure (Redis, RabbitMQ, DB) is required.
 */
@WebMvcTest(controllers = { EventController.class, GlobalExceptionHandler.class })
class EventControllerTest {

    private static final String BASE_URL = "/api/events";
    private static final String EVENT_ID = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
    private static final String OTHER_EVENT_ID = "a3f1c847-2d6e-4b89-9f0a-1c2d3e4f5a6b";
    private static final String USER_ID = "7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a";
    private static final String REQUEST_ID = "b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    // -------------------------------------------------------------------------
    // GET /{eventId}/tickets/availability
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /{eventId}/tickets/availability")
    class CheckAvailability {

        @Test
        @DisplayName("200 OK — returns availability response from service")
        void shouldReturn200WithAvailabilityResponse() throws Exception {
            // given
            TicketAvailabilityResponse response = new TicketAvailabilityResponse(EVENT_ID, 150);
            when(eventService.getAvailabilityCount(EVENT_ID)).thenReturn(response);

            // when / then
            mockMvc.perform(get(BASE_URL + "/{eventId}/tickets/availability", EVENT_ID)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                    .andExpect(jsonPath("$.availableTickets").value(150));

            verify(eventService).getAvailabilityCount(EVENT_ID);
        }

        @Test
        @DisplayName("404 Not Found - service throws EventNotFoundException")
        void shouldReturn404WhenEventNotFound() throws Exception {
            // given
            when(eventService.getAvailabilityCount(EVENT_ID))
                    .thenThrow(new EventNotFoundException(EVENT_ID));

            // when / then
            mockMvc.perform(get(BASE_URL + "/{eventId}/tickets/availability", EVENT_ID)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(eventService).getAvailabilityCount(EVENT_ID);
        }
    }

    // -------------------------------------------------------------------------
    // POST /{eventId}/tickets/order
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /{eventId}/tickets/order")
    class PlaceOrder {

        @Test
        @DisplayName("202 Accepted — valid request is forwarded to service")
        void shouldReturn202ForValidOrder() throws Exception {
            // given
            TicketOrderRequest request = new TicketOrderRequest(EVENT_ID, USER_ID, 2, REQUEST_ID);
            doNothing().when(eventService).createOrder(any(TicketOrderRequest.class));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            verify(eventService).createOrder(any(TicketOrderRequest.class));
        }

        @Test
        @DisplayName("400 Bad Request — eventId in path does not match body")
        void shouldReturn400WhenEventIdMismatch() throws Exception {
            // given — body carries a different event ID
            TicketOrderRequest request = new TicketOrderRequest(OTHER_EVENT_ID, USER_ID, 2, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — eventId in body is blank (@NotBlank)")
        void shouldReturn400WhenEventIdIsBlank() throws Exception {
            // given
            String json = """
                    {
                        "eventId": "",
                        "userId": "%s",
                        "quantity": 2,
                        "requestId": "%s"
                    }
                    """.formatted(USER_ID, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — userId is blank (@NotBlank)")
        void shouldReturn400WhenUserIdIsBlank() throws Exception {
            // given
            String json = """
                    {
                        "eventId": "%s",
                        "userId": "   ",
                        "quantity": 2,
                        "requestId": "%s"
                    }
                    """.formatted(EVENT_ID, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — quantity is null (@NotNull)")
        void shouldReturn400WhenQuantityIsNull() throws Exception {
            // given
            String json = """
                    {
                        "eventId": "%s",
                        "userId": "%s",
                        "quantity": null,
                        "requestId": "%s"
                    }
                    """.formatted(EVENT_ID, USER_ID, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — quantity is 0 (@Min(1))")
        void shouldReturn400WhenQuantityIsZero() throws Exception {
            // given
            String json = """
                    {
                        "eventId": "%s",
                        "userId": "%s",
                        "quantity": 0,
                        "requestId": "%s"
                    }
                    """.formatted(EVENT_ID, USER_ID, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — quantity is negative (@Min(1))")
        void shouldReturn400WhenQuantityIsNegative() throws Exception {
            // given
            String json = """
                    {
                        "eventId": "%s",
                        "userId": "%s",
                        "quantity": -5,
                        "requestId": "%s"
                    }
                    """.formatted(EVENT_ID, USER_ID, REQUEST_ID);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("400 Bad Request — request body is missing entirely")
        void shouldReturn400WhenBodyIsMissing() throws Exception {
            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(eventService, never()).createOrder(any());
        }

        @Test
        @DisplayName("202 Accepted — requestId is optional (null is allowed)")
        void shouldReturn202WhenRequestIdIsNull() throws Exception {
            // requestId has no validation, null is valid
            String json = """
                    {
                        "eventId": "%s",
                        "userId": "%s",
                        "quantity": 1
                    }
                    """.formatted(EVENT_ID, USER_ID);
            doNothing().when(eventService).createOrder(any(TicketOrderRequest.class));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{eventId}/tickets/order", EVENT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isAccepted());

            verify(eventService).createOrder(any(TicketOrderRequest.class));
        }
    }
}
