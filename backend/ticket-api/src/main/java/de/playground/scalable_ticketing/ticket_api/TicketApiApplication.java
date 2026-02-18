package de.playground.scalable_ticketing.ticket_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(title = "Scalable Ticketing System", version = "1.0", description = "Private playground for learning concepts and technologies for high-available distributed systems. Ticket Use Cases: 1. Check ticket availability (High Frequency Read) 2. Buy tickets (High Concurrency Write)"))
@SpringBootApplication
// Use Entities and Repositorys from shared module
@EntityScan(basePackages = {
		"de.playground.scalable_ticketing.ticket_api",
		"de.playground.scalable_ticketing.common.domain.model"
})
@EnableJpaRepositories(basePackages = {
		"de.playground.scalable_ticketing.common.domain.repository"
})
public class TicketApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketApiApplication.class, args);
	}

}
