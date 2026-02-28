package de.playground.scalable_ticketing.ticket_api;

import de.playground.scalable_ticketing.ticket_api.config.TestApiInfrastructureConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@Import(TestApiInfrastructureConfig.class)
@ContextConfiguration(initializers = TestApiInfrastructureConfig.RedisInitializer.class)
class TicketApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
