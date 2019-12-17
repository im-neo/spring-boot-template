package queue;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class Consumer implements Runnable {

    private BlockingQueue<QueueData> queue;
    private static final int SLEEP_TIME = 1000;

    public Consumer(BlockingQueue<QueueData> queue) {
        this.queue = queue;
    }


    @Override
    public void run() {
        System.out.println("Consumer Thread id: " + Thread.currentThread().getId() + " started!");
        Random random = new Random();

        while (true) {
            try {
                QueueData data = queue.take();
                if (data != null) {
                    //取出数据成功,输出计算平方值
                    log.info("{}计算平方:{} * {} = {}", Thread.currentThread().getId(), data.getNum(), data.getNum(), data.getNum() * data.getNum());
                    Thread.sleep(random.nextInt(SLEEP_TIME));  //随机睡眠一定时间，模拟任务执行
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.interrupted();
            }
        }
    }
}
