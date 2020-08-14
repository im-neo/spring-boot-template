package com.neo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_TOPIC_INFORM = "exchange_topic_inform";

    public static final String QUEUE_INFORM_EMAIL = "queue_inform_email";
    public static final String QUEUE_INFORM_SMS = "queue_inform_sms";

    public static final String ROUTING_KEY_EMAIL = "inform.#.email.#";
    public static final String ROUTING_KEY_SMS = "inform.#.sms.#";

    //声明队列
    @Bean
    public Queue queueEmail() {
        return new Queue(QUEUE_INFORM_EMAIL);
    }

    //声明Topic交换机
    @Bean
    Exchange topicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_TOPIC_INFORM).durable(true).build();
    }

    //将队列与Topic交换机进行绑定，并指定路由键
    @Bean
    Binding topicBindingEmail(@Qualifier("queueEmail") Queue queue, @Qualifier("topicExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_EMAIL).noargs();
    }

}
