package de.playground.scalable_ticketing.ticket_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import de.playground.scalable_ticketing.common.dto.TicketPurchaseRequest; // TODO: Delete this



@SpringBootApplication
public class TicketApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketApiApplication.class, args);
	}

}
