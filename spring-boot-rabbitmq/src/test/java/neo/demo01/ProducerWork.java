package neo.demo01;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * RabbitMQ 入门程序生产者
 * Work queue 模式
 */
public class ProducerWork {

    public static final String QUEUE = "hello.world";


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
            channel.queueDeclare(QUEUE, true, false, false, null);
            /**
             * 发送消息
             * String exchange, String routingKey, boolean mandatory, BasicProperties props, byte[] body
             * exchange - 交换机，如果不指定将使用MQ的默认交换机
             * routingKey - 路由Key，交换机根据路由Key来将消息转发到指定队列，如果使用默认交换机，routingKey 设置为队列名称
             * props - 消息属性
             * body - 消息内容
             */
            for (int i = 0; i < 10; i++) {
                String message = "hello , i'm neo" + i;
                channel.basicPublish(StringUtils.EMPTY, QUEUE, null, message.getBytes());
                System.out.println("send to mq : " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
