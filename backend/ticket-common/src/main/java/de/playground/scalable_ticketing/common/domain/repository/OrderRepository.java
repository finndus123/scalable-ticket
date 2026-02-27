package de.playground.scalable_ticketing.common.domain.repository;

import de.playground.scalable_ticketing.common.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Order entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
}
