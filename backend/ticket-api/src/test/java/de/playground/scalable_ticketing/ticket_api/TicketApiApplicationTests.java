package de.playground.scalable_ticketing.ticket_api;

import de.playground.scalable_ticketing.ticket_api.config.TestInfrastructureConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@Import(TestInfrastructureConfig.class)
@ContextConfiguration(initializers = TestInfrastructureConfig.RedisInitializer.class)
class TicketApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
