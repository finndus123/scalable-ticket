package de.playground.scalable_ticketing.ticket_api.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for JPA persistence including entity scanning and repository support from shared module
 */
@Configuration
@EntityScan(basePackages = {
        "de.playground.scalable_ticketing.ticket_api",
        "de.playground.scalable_ticketing.common.domain.model"
})
@EnableJpaRepositories(basePackages = {
        "de.playground.scalable_ticketing.common.domain.repository"
})
public class JpaConfig {
}
