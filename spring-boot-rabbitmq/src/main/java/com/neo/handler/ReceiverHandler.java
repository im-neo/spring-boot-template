package com.neo.handler;

import com.neo.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReceiverHandler {


    @RabbitListener(queues = {RabbitMQConfig.QUEUE_INFORM_EMAIL})
    public void receiveEmail(String msg, Message message, Channel channel) {
        System.out.println("received message : " + msg);
    }

}
