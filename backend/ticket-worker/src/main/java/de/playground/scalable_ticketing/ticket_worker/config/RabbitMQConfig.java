package de.playground.scalable_ticketing.ticket_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ integration.
 * Defines the exchange, queue, binding, message conversion, and listener limits.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue}")
    private String queue;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    @Value("${rabbitmq.prefetch-count}")
    private int prefetchCount;

    /**
     * Explicitly declare the Topic Exchange to ensure the topology exists.
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
     * Explicitly declare the durable Queue to ensure the topology exists.
     * Configured with a Dead Letter Exchange to handle failed messages.
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
     * Explicitly declare the Binding for the main queue.
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
     * Configures the MessageConverter to use JSON serialization for receiving payloads.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Creates the SimpleRabbitListenerContainerFactory for receiving messages.
     * Configured with the JSON message converter and application.yaml properties (like retries).
     *
     * @param connectionFactory The ConnectionFactory to use.
     * @param configurer for setting configuration properties.
     * @return The configured SimpleRabbitListenerContainerFactory.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(prefetchCount);
        return factory;
    }
}
