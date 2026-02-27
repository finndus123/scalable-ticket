package de.playground.scalable_ticketing.ticket_api.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.test.TestRabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;


/**
 * Test configuration for setting up test infrastructure for Integration-SpringBootTests
 * Config includes Redis and RabbitMQ:
 * - Redis is started using Testcontainers.
 * - RabbitMQ is mocked using Mockito.
 */
@TestConfiguration
public class TestInfrastructureConfig {

    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
                .withExposedPorts(6379);

        static {
            redis.start();
        }

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            Map<String, Object> properties = Map.of(
                    "spring.data.redis.host", redis.getHost(),
                    "spring.data.redis.port", redis.getMappedPort(6379).toString());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testcontainers", properties));
        }
    }

    @Bean
    public TestRabbitTemplate template() throws IOException {
        return new TestRabbitTemplate(connectionFactory());
    }

    @Bean
    public ConnectionFactory connectionFactory() throws IOException {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        Channel channel = mock(Channel.class);
        willReturn(connection).given(factory).createConnection();
        willReturn(channel).given(connection).createChannel(anyBoolean());
        given(channel.isOpen()).willReturn(true);
        return factory;
    }
}
