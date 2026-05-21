package io.github.mystagogy.savepocket.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "notification.rabbitmq", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(NotificationRabbitProperties.class)
public class NotificationRabbitConfig {

    @Bean
    public DirectExchange notificationExchange(NotificationRabbitProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue notificationDlq(NotificationRabbitProperties properties) {
        return new Queue(properties.dlq(), true);
    }

    @Bean
    public Queue notificationQueue(NotificationRabbitProperties properties) {
        return new Queue(
                properties.queue(),
                true,
                false,
                false,
                Map.of("x-dead-letter-exchange", "", "x-dead-letter-routing-key", properties.dlq())
        );
    }

    @Bean
    public Binding notificationBinding(
            Queue notificationQueue,
            DirectExchange notificationExchange,
            NotificationRabbitProperties properties
    ) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(properties.routingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
