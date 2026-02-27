package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Ticket entities.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}
