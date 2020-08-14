package neo.demo04;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.ExchangeTypes;

/**
 * RabbitMQ 入门程序生产者
 */
public class ProducerTopic {


    public static final String EXCHANGE_TOPIC_INFORM = "exchange_topic_inform";

    public static final String QUEUE_INFORM_EMAIL = "queue_inform_email";
    public static final String QUEUE_INFORM_SMS = "queue_inform_sms";

    public static final String ROUTING_KEY_EMAIL = "inform.#.email.#";
    public static final String ROUTING_KEY_SMS = "inform.#.sms.#";


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
            /**
             * String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
             * queue - 队列名称
             * durable -  是否持久化，重启后数据还在
             * exclusive - 是否独占队列，队列只允许在该连接中访问，如果 connection 连接关闭队列自动删除，如果将此参数设置为 true ,可用于临时队列的创建
             * autoDelete - 是否自动删除，队列不再使用时是否自动删除此队列，如果参数 exclusive 设置为 true 就可以实现临时队列
             * arguments - 参数，可以设置队列的扩展参数，如：存活时间
             */
            channel.queueDeclare(QUEUE_INFORM_EMAIL, true, false, false, null);
            channel.queueDeclare(QUEUE_INFORM_SMS, true, false, false, null);

            /**
             * 申明一个交换机
             * String exchange, String type
             * exchange - 交换机名称
             * type - ExchangeTypes 交换机类型
             *          DIRECT      - 路由工作模式
             *          FANOUT      - 发布订阅模式（pub/sub）
             *          TOPIC       - 
             *          HEADERS     - 
             *          SYSTEM      - 
             */
            channel.exchangeDeclare(EXCHANGE_TOPIC_INFORM, ExchangeTypes.TOPIC);


            channel.queueBind(QUEUE_INFORM_EMAIL, EXCHANGE_TOPIC_INFORM, ROUTING_KEY_EMAIL);
            channel.queueBind(QUEUE_INFORM_SMS, EXCHANGE_TOPIC_INFORM, ROUTING_KEY_SMS);


            /**
             * 发送消息
             * String exchange, String routingKey, boolean mandatory, BasicProperties props, byte[] body
             * exchange - 交换机，如果不指定将使用MQ的默认交换机
             * routingKey - 路由Key，交换机根据路由Key来将消息转发到指定队列，如果使用默认交换机，routingKey 设置为队列名称
             * props - 消息属性
             * body - 消息内容
             */
            for (int i = 0; i < 10; i++) {
                String message = "send email inform message to user-" + i;
                channel.basicPublish(EXCHANGE_TOPIC_INFORM, "inform.email", null, message.getBytes());
                System.out.println("send to mq : " + message);
            }
            for (int i = 0; i < 10; i++) {
                String message = "send sms inform message to user-" + i;
                channel.basicPublish(EXCHANGE_TOPIC_INFORM, "inform.sms", null, message.getBytes());
                System.out.println("send to mq : " + message);
            }
            for (int i = 0; i < 10; i++) {
                String message = "send sms and email inform message to user-" + i;
                channel.basicPublish(EXCHANGE_TOPIC_INFORM, "inform.sms.email", null, message.getBytes());
                System.out.println("send to mq : " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
