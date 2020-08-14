package neo.demo02;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ 入门程序消费者
 */
public class ConsumerEmail {

    public static final String QUEUE_INFORM_EMAIL = "queue_inform_email";
    public static final String EXCHANGE_FANOUT_INFORM = "exchange_fanout_inform";


    public static void main(String[] args) {
        // 1.和MQ建立连接
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        // 设置虚拟机，一个MQ服务可以设置多个虚拟机，没个MQ就相当于一个独立的MQ
        connectionFactory.setVirtualHost("/");

        // 建立新的连接
        try (Connection connection = connectionFactory.newConnection()) {
            // 创建会话通道，生产者和MQ服务的所有通信都通过 channel
            Channel channel = connection.createChannel();

            channel.queueDeclare(QUEUE_INFORM_EMAIL, true, false, false, null);

            channel.exchangeDeclare(EXCHANGE_FANOUT_INFORM, ExchangeTypes.FANOUT);
            
            channel.queueBind(QUEUE_INFORM_EMAIL, EXCHANGE_FANOUT_INFORM, StringUtils.EMPTY);

            // 实现消费方法
            DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
                /**
                 * 当接收到消息时执行此方法
                 * @param consumerTag 消费者标签，用来标识消费者
                 * @param envelope 获取AMQP的基本方法一组参数
                 * @param properties 消息属性
                 * @param body 消息内容
                 */
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    // 交换机信息
                    String exchange = envelope.getExchange();

                    // 消息的ID
                    long deliveryTag = envelope.getDeliveryTag();


                    String message = new String(body, StandardCharsets.UTF_8);
                    System.out.println("received message : " + message);
                }
            };

            /**
             * 监听队列
             * String queue, boolean autoAck, Consumer callback
             * queue - 队列名称
             * autoAck - 是否自动回复，但设置为 true ,消费者接收到消息后要告诉MQ消息已接收,否则自动回复，次设置可以保证消息不丢失
             * callback - 消费方法，消费者接收到消息，需要执行的方法
             */
            channel.basicConsume(QUEUE_INFORM_EMAIL , true ,defaultConsumer);
            while (true){
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
