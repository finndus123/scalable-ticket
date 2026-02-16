package de.playground.scalable_ticketing.ticket_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
