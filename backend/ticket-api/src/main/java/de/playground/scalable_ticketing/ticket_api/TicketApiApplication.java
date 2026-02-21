package de.playground.scalable_ticketing.ticket_api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Scalable Ticketing System", version = "1.0", description = "Private playground for learning concepts and technologies for high-available distributed systems. Ticket Use Cases: 1. Check ticket availability (High Frequency Read) 2. Buy tickets (High Concurrency Write)"))
@SpringBootApplication(scanBasePackages = { "de.playground.scalable_ticketing.ticket_api",
		"de.playground.scalable_ticketing.common" })
public class TicketApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketApiApplication.class, args);
	}

}
