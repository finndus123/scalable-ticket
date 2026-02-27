package de.playground.scalable_ticketing.ticket_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@EnableResilientMethods
@SpringBootApplication(scanBasePackages = {"de.playground.scalable_ticketing.ticket_worker", "de.playground.scalable_ticketing.common"})
public class TicketWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketWorkerApplication.class, args);
    }

}
