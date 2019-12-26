package queue;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
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
        ExecutorService service = Executors.newFixedThreadPool(3);
        service.execute(producer1);
        service.execute(producer2);
        service.execute(producer3);
        service.execute(consumer1);
        service.execute(consumer2);
        service.execute(consumer3);
        Thread.sleep(10 * 1000);  //执行生产者和消费者
        producer1.stop();
        producer2.stop();
        producer3.stop();    //停止生产任务
        service.shutdown();

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) service;

        while (true) {
            int queueSize = threadPoolExecutor.getQueue().size();
            int activeCount = threadPoolExecutor.getActiveCount();
            long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
            long taskCount = threadPoolExecutor.getTaskCount();

            log.info("当前排队线程数：{}，当前活动线程数：{}，执行完成线程数：{}，总线程数：{}", queueSize, activeCount, completedTaskCount, taskCount);
            Thread.sleep(1000 * 2);
        }
    }

}
