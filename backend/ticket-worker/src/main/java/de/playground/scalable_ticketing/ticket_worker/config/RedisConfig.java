package de.playground.scalable_ticketing.ticket_worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration class for Redis integration in ticket-worker.
 */
@Configuration
public class RedisConfig {

    /**
     * Configures and returns a RedisTemplate for interacting with Redis.
     * Uses String serialization for keys, allowing direct pattern/key deletion matching the API.
     *
     * @param connectionFactory the connection factory to use
     * @param objectMapper      the object mapper for JSON serialization
     * @return the configured RedisTemplate
     */
    @Bean
    RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String serialization for cache invalidation (event:{id})
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Standard JSON serialization for values (to mirror API's structure)
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
