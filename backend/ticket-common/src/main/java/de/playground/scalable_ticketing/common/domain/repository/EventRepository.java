package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Shared repository for Event entities.
 * Acts as the single source of truth for database access to events.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Acquires a database write lock on the event row so concurrent workers serialize availability updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT e.availableTickets FROM Event e WHERE e.id = :id")
    Optional<Integer> findAvailableTicketsById(@Param("id") UUID id);
}
