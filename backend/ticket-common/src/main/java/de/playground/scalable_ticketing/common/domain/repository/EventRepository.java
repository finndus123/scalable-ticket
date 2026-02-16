package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Shared repository for Event entities.
 * Acts as the single source of truth for database access to events.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, String> {
}
