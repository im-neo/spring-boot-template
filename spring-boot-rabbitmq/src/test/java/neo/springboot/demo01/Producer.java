package neo.springboot.demo01;

import com.neo.config.RabbitMQConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * RabbitTemplate
     */
    @Test
    public void testSendEmail() {
        for (int i = 0; i < 10; i++) {
            String message = "email message to user-" + i;
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_TOPIC_INFORM, RabbitMQConfig.ROUTING_KEY_EMAIL, message);
        }
    }
}
