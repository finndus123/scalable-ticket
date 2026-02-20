package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Event;
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
public interface EventRepository extends JpaRepository<Event, String> {

    @Query("SELECT e.availableTickets FROM Event e WHERE e.id = :id")
    Optional<Integer> findAvailableTicketsById(@Param("id") UUID id);
}
