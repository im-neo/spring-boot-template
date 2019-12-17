package queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueTest {

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<QueueData> queue = new LinkedBlockingQueue<>(10);
        //新建3个生产者线程和3个消费者线程
        Producer producer1 = new Producer(queue);
        Producer producer2 = new Producer(queue);
        Producer producer3 = new Producer(queue);
        Consumer consumer1 = new Consumer(queue);
        Consumer consumer2 = new Consumer(queue);
        Consumer consumer3 = new Consumer(queue);
        //线程池
        ExecutorService service = Executors.newFixedThreadPool(1);
        service.execute(producer1);
        service.execute(producer2);
        service.execute(producer3);
        service.execute(consumer1);
        service.execute(consumer2);
        service.execute(consumer3);
        Thread.sleep(10 * 1000);  //执行生产者和消费者
        producer1.stop();
        producer2.stop();
        producer3.stop();	//停止生产任务
        service.shutdown();
        while (service.isTerminated()){
            Thread.sleep(2);
        }
        System.out.println("结束");
    }

}
