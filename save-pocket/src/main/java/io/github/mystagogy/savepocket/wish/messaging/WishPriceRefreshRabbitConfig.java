package io.github.mystagogy.savepocket.wish.messaging;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "wish.price-refresh.rabbitmq", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WishPriceRefreshRabbitProperties.class)
public class WishPriceRefreshRabbitConfig {

    @Bean
    public DirectExchange wishPriceRefreshExchange(WishPriceRefreshRabbitProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue wishPriceRefreshDlq(WishPriceRefreshRabbitProperties properties) {
        return new Queue(properties.dlq(), true);
    }

    @Bean
    public Queue wishPriceRefreshQueue(WishPriceRefreshRabbitProperties properties) {
        return new Queue(
                properties.queue(),
                true,
                false,
                false,
                Map.of("x-dead-letter-exchange", "", "x-dead-letter-routing-key", properties.dlq())
        );
    }

    @Bean
    public Binding wishPriceRefreshBinding(
            Queue wishPriceRefreshQueue,
            DirectExchange wishPriceRefreshExchange,
            WishPriceRefreshRabbitProperties properties
    ) {
        return BindingBuilder.bind(wishPriceRefreshQueue).to(wishPriceRefreshExchange).with(properties.routingKey());
    }
}
