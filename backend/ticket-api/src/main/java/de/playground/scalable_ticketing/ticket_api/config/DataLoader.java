package de.playground.scalable_ticketing.ticket_api.config;

import de.playground.scalable_ticketing.common.domain.model.Event;
import de.playground.scalable_ticketing.common.domain.model.Ticket;
import de.playground.scalable_ticketing.common.domain.model.TicketStatus;
import de.playground.scalable_ticketing.common.domain.model.User;
import de.playground.scalable_ticketing.common.domain.repository.EventRepository;
import de.playground.scalable_ticketing.common.domain.repository.TicketRepository;
import de.playground.scalable_ticketing.common.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Seeding script, is enabled if seed.enabled is set to true
 * Creates sample User, Event and available Tickets
 */
@Component
@ConditionalOnProperty(
        name = "seed.enabled",
        havingValue = "true"
)
public class DataLoader implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public DataLoader(
            TicketRepository ticketRepository,
            EventRepository eventRepository,
            UserRepository userRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0 && eventRepository.count() == 0 && ticketRepository.count() == 0) {
            User homer = new User(UUID.fromString("7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a"), "Homer Simpson", "homer.simpson@nuclear.com");
            User mrBurns = new User(UUID.fromString("b1c2d3e4-f5a6-7b8c-9d0e-1f2a3b4c5d6e"), "Montgomery Burns", "mr.burns@nuclear.com");
            userRepository.saveAll(List.of(mrBurns, homer));

            UUID krustyShowId = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3");
            Event krustyShow = new Event(krustyShowId, "Krusty the Clown Show", "Channel 6 Studios", 50000, 50000, BigDecimal.valueOf(15.00));
            eventRepository.save(krustyShow);

            List<Ticket> tickets = new ArrayList<>();
            for (int i = 0; i < krustyShow.getAvailableTickets(); i++) {
                tickets.add(new Ticket(UUID.randomUUID(), krustyShowId, TicketStatus.AVAILABLE));
            }
            ticketRepository.saveAll(tickets);
        }

    }
}
