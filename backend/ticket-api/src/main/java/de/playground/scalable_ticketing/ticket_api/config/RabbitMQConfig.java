package de.playground.scalable_ticketing.ticket_api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ integration.
 * Defines the exchange, queue, binding, and message conversion settings.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue}")
    private String queue;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    /**
     * Creates the Topic Exchange for ticket operations.
     *
     * @return The configured TopicExchange.
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    /**
     * Explicitly declare the Dead Letter Exchange (DLX).
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(exchange + ".dlx");
    }

    /**
     * Creates the durable Queue for ticket orders.
     * Configured with a Dead Letter Exchange to handle failed messages.
     *
     * @return The configured Queue.
     */
    @Bean
    public Queue queue() {
        return org.springframework.amqp.core.QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    /**
     * Explicitly declare the Dead Letter Queue (DLQ).
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(queue + ".dlq", true);
    }

    /**
     * Binds the Queue to the Exchange using the routing key.
     *
     * @param queue    The queue to bind.
     * @param exchange The exchange to bind to.
     * @return The Binding definition.
     */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    /**
     * Explicitly declare the Binding for the DLQ.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }

    /**
     * Configures the MessageConverter to use JSON serialization using Jackson.
     *
     * @return The JacksonJsonMessageConverter instance.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Creates the RabbitTemplate for sending messages.
     * Configured with the JSON message converter.
     *
     * @param connectionFactory The ConnectionFactory to use.
     * @return The configured RabbitTemplate.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
