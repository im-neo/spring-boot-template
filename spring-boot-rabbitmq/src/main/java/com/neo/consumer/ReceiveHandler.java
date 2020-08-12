package com.neo.consumer;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReceiveHandler {
 
    //监听邮件队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue_email", durable = "true"),
            exchange = @Exchange(
                    value = "topic.exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC
            ),
            key = {"topic.#.email.#","email.*"}))
    public void receiveEmail(String msg){
        System.out.println(" [邮件服务] received : " + msg + "!");
    }
 
    //监听短信队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue_sms", durable = "true"),
            exchange = @Exchange(
                    value = "topic.exchange",
                    ignoreDeclarationExceptions = "true",
                    type = ExchangeTypes.TOPIC
            ),
            key = {"topic.#.sms.#"}))
    public void receiveSms(String msg){
        System.out.println(" [短信服务] received : " + msg + "!");
    }
}