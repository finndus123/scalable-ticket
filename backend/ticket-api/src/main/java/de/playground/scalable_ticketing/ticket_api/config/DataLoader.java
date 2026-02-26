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

    public DataLoader(TicketRepository ticketRepository,
                      EventRepository eventRepository,
                      UserRepository userRepository){
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if(userRepository.count() == 0 && eventRepository.count() == 0 && ticketRepository.count() == 0) {
            User homer = new User(UUID.randomUUID(), "Homer Simpson", "homer.simpson@nuclear.com");
            User mrBurns = new User(UUID.randomUUID(), "Montgomery Burns", "mr.burns@nuclear.com");
            userRepository.saveAll(List.of(mrBurns, homer));

            UUID krustyShowId = UUID.randomUUID();
            Event krustyShow = new Event(krustyShowId, "Krusty the Clown Show", "Channel 6 Studios", 10000, 10000, BigDecimal.valueOf(15.00));
            eventRepository.save(krustyShow);

            // Todo: Simplify Example by creating tickets after order creation
            List<Ticket> tickets = new ArrayList<>();
            for(int i = 0; i < krustyShow.getAvailableTickets(); i++) {
                tickets.add(new Ticket(UUID.randomUUID(), krustyShowId, TicketStatus.AVAILABLE));
            }
            ticketRepository.saveAll(tickets);
        }

    }
}
