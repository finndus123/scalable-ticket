package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Ticket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Ticket entities.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /**
     * Finds available tickets for a given event, limited by the provided {@link Pageable}.
     *
     * @param eventId  the event to find tickets for
     * @param pageable controls the maximum number of tickets returned
     * @return a list of available tickets, up to the limit specified by pageable
     */
    @Query("SELECT t FROM Ticket t WHERE t.eventId = :eventId AND t.status = 'AVAILABLE' ORDER BY t.id")
    List<Ticket> findAvailableByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    /**
     * Locks only currently free ticket rows and skips rows already locked by another transaction.
     */
    @Query(value = """
            SELECT *
            FROM tickets t
            WHERE t.event_id = :eventId
              AND t.status = 'AVAILABLE'
            ORDER BY t.id
            LIMIT :quantity
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Ticket> findAvailableByEventIdForUpdate(@Param("eventId") UUID eventId, @Param("quantity") int quantity);
}

